package com.triptyche.backend.domain.media.service;

import com.triptyche.backend.domain.media.dto.MediaBatchDeleteRequest;
import com.triptyche.backend.domain.media.dto.MediaBatchEditRequest;
import com.triptyche.backend.domain.media.dto.MediaLocationEditRequest;
import com.triptyche.backend.domain.media.dto.MediaUploadRequest;
import com.triptyche.backend.domain.media.dto.MediaProcessingRequest;
import com.triptyche.backend.domain.media.dto.MediaProcessingResponse;
import com.triptyche.backend.domain.media.event.MediaFileAddedEvent;
import com.triptyche.backend.domain.media.event.MediaFileDeletedEvent;
import com.triptyche.backend.domain.media.event.MediaFileLocationUpdatedEvent;
import com.triptyche.backend.domain.media.event.MediaFileRegisteredEvent;
import com.triptyche.backend.domain.media.event.MediaFileUpdatedEvent;
import com.triptyche.backend.domain.media.event.MediaFileZeroLocationCacheRequestedEvent;
import com.triptyche.backend.domain.media.event.MediaFilesS3DeleteRequestedEvent;
import com.triptyche.backend.domain.media.event.MediaLocationCacheEvictRequestedEvent;
import com.triptyche.backend.domain.media.model.MediaFile;
import com.triptyche.backend.domain.media.repository.MediaFileRepository;
import com.triptyche.backend.domain.trip.model.PinPoint;
import com.triptyche.backend.domain.trip.model.Trip;
import com.triptyche.backend.domain.trip.service.PinPointService;
import com.triptyche.backend.domain.trip.service.TripAccessGuard;
import com.triptyche.backend.domain.user.model.User;
import com.triptyche.backend.global.common.ResultCode;
import com.triptyche.backend.global.exception.CustomException;
import com.triptyche.backend.global.s3.S3KeyResolver;
import com.triptyche.backend.global.util.DateFormatter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MediaCommandService {

  private final MediaFileRepository mediaFileRepository;
  private final PinPointService pinPointService;
  private final S3KeyResolver s3KeyResolver;
  private final TripAccessGuard tripAccessGuard;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional
  public void processAndSaveMetadataBatch(User user, String tripKey, List<MediaUploadRequest> files) {
    Trip trip = tripAccessGuard.validateAccessibleTripByKey(tripKey, user);

    boolean isOwner = trip.isOwner(user);

    // fileKey 형식은 dedup 이전에 검증 (잘못된 키가 섞여 들어오면 빠르게 거부).
    files.forEach(file -> {
      if (!S3KeyResolver.isOriginalKey(file.fileKey())) {
        throw new CustomException(ResultCode.INVALID_FILE_KEY);
      }
    });

    // 동일 tripKey로 metadata POST가 반복돼도 같은 fileKey가 중복 저장되지 않도록 사전 dedup.
    // UNIQUE 인덱스(uq_media_file_trip_id_media_key)와 함께 idempotent 등록을 보장.
    Set<String> incomingKeys = files.stream()
            .map(MediaUploadRequest::fileKey)
            .collect(Collectors.toSet());
    Set<String> existingKeys = incomingKeys.isEmpty()
            ? Set.of()
            : new HashSet<>(mediaFileRepository.findExistingMediaKeys(trip.getTripId(), incomingKeys));

    List<MediaUploadRequest> newFiles = files.stream()
            .filter(f -> !existingKeys.contains(f.fileKey()))
            .toList();

    if (newFiles.isEmpty()) {
      // 모든 fileKey가 이미 등록된 상태 → 새 INSERT/이벤트 없이 조용히 성공 처리(완전 idempotent).
      return;
    }

    List<PinPoint> existingPinPoints = pinPointService.findAllByTripId(trip.getTripId());

    List<MediaFile> mediaFiles = newFiles.stream()
            .map(file -> {
              LocalDateTime recordDateTime = DateFormatter.convertToLocalDateTime(file.recordDate());
              PinPoint pinPoint = pinPointService.assignPinPoint(
                      existingPinPoints, trip, file.latitude(), file.longitude());
              return MediaFile.builder()
                      .trip(trip)
                      .pinPoint(pinPoint)
                      .mediaType("image/webp")
                      .mediaLink(s3KeyResolver.buildUrl(file.fileKey()))
                      .mediaKey(file.fileKey())
                      .recordDate(recordDateTime)
                      .latitude(file.latitude())
                      .longitude(file.longitude())
                      .build();
            })
            .toList();
    List<MediaFile> savedMediaFiles = mediaFileRepository.saveAll(mediaFiles);

    savedMediaFiles.forEach(mediaFile ->
            eventPublisher.publishEvent(
                    MediaFileRegisteredEvent.v1(mediaFile.getMediaFileId(), mediaFile.getMediaKey()))
    );

    // Redis 처리 (위치 0.0인 파일들만) — DB 커밋 완료 후 이벤트로 처리
    savedMediaFiles.stream()
            .filter(mf -> mf.getLatitude() == 0.0 && mf.getLongitude() == 0.0)
            .forEach(mf -> eventPublisher.publishEvent(new MediaFileZeroLocationCacheRequestedEvent(
                    trip.getTripId(),
                    mf.getMediaFileId(),
                    mf.getMediaLink(),
                    mf.getRecordDate().toString()
            )));

    eventPublisher.publishEvent(new MediaFileAddedEvent(
            trip.getTripId(),
            trip.getTripTitle(),
            trip.getTripKey(),
            trip.getUser().getUserId(),
            user.getUserId(),
            user.getUserNickName(),
            isOwner,
            savedMediaFiles.size()));
  }

  @Transactional
  public int updateMultipleMediaFiles(User user, String tripKey, MediaBatchEditRequest request) {
    Trip trip = tripAccessGuard.validateAccessibleTripByKey(tripKey, user);
    Long actorId = user.getUserId();
    String actorNickname = user.getUserNickName();
    boolean isOwner = trip.isOwner(user);

    List<PinPoint> existingPinPoints = pinPointService.findAllByTripId(trip.getTripId());

    List<Long> mediaFileIds = request.mediaFiles().stream()
            .map(MediaBatchEditRequest.MediaFileUpdateRequest::mediaFileId)
            .toList();

    Map<Long, MediaFile> mediaFileMap = mediaFileRepository.findAllById(mediaFileIds).stream()
            .collect(Collectors.toMap(MediaFile::getMediaFileId, mf -> mf));

    if (mediaFileMap.size() != mediaFileIds.size()) {
      throw new CustomException(ResultCode.MEDIA_FILE_NOT_FOUND);
    }

    List<MediaFile> updatedMediaFiles = request.mediaFiles().stream()
            .map(fileUpdate -> {
              MediaFile mf = mediaFileMap.get(fileUpdate.mediaFileId());
              PinPoint pinPoint = pinPointService.assignPinPoint(
                      existingPinPoints, trip, fileUpdate.latitude(), fileUpdate.longitude());
              mf.updateRecordDate(fileUpdate.recordDate());
              mf.updateLocation(fileUpdate.latitude(), fileUpdate.longitude(), pinPoint);
              return mf;
            })
            .toList();

    eventPublisher.publishEvent(new MediaFileUpdatedEvent(
            trip.getTripId(),
            trip.getTripTitle(),
            trip.getTripKey(),
            trip.getUser().getUserId(),
            actorId,
            actorNickname,
            isOwner,
            updatedMediaFiles.size()
    ));
    return updatedMediaFiles.size();
  }

  @Transactional
  public int deleteMultipleMediaFiles(User user, String tripKey, MediaBatchDeleteRequest request) {
    Trip trip = tripAccessGuard.validateAccessibleTripByKey(tripKey, user);
    Long actorId = user.getUserId();
    String actorNickname = user.getUserNickName();
    boolean isOwner = trip.isOwner(user);

    List<MediaFile> mediaFiles = mediaFileRepository.findAllById(request.mediaFileIds());

    List<String> mediaKeys = mediaFiles.stream()
            .flatMap(mf -> {
              List<String> keys = new ArrayList<>();
              if (mf.getMediaKey() != null) {
                keys.add(mf.getMediaKey());
              }
              String webpKey = s3KeyResolver.extractKey(mf.getMediaLink());
              if (webpKey != null && !webpKey.equals(mf.getMediaKey())) {
                keys.add(webpKey);
              }
              return keys.stream();
            })
            .distinct()
            .toList();

    mediaFileRepository.deleteAll(mediaFiles);

    eventPublisher.publishEvent(new MediaFilesS3DeleteRequestedEvent(mediaKeys));

    eventPublisher.publishEvent(new MediaFileDeletedEvent(
            trip.getTripId(),
            trip.getTripTitle(),
            trip.getTripKey(),
            trip.getUser().getUserId(),
            actorId,
            actorNickname,
            isOwner,
            mediaFiles.size()
    ));

    return mediaFiles.size();
  }

  @Transactional
  public void updateImageLocation(User user,
                                  String tripKey,
                                  Long mediaFileId,
                                  MediaLocationEditRequest request) {
    Trip trip = tripAccessGuard.validateAccessibleTripByKey(tripKey, user);
    MediaFile mf = mediaFileRepository.findById(mediaFileId)
            .orElseThrow(() -> new CustomException(ResultCode.MEDIA_FILE_NOT_FOUND));

    PinPoint pinPoint = pinPointService.assignPinPointWithQuery(trip, request.latitude(), request.longitude());
    mf.updateLocation(request.latitude(), request.longitude(), pinPoint);

    eventPublisher.publishEvent(new MediaLocationCacheEvictRequestedEvent(trip.getTripId(), mediaFileId));

    Long actorId = user.getUserId();
    String actorNickname = user.getUserNickName();
    boolean isOwner = trip.isOwner(user);
    eventPublisher.publishEvent(new MediaFileLocationUpdatedEvent(
            trip.getTripId(),
            trip.getTripTitle(),
            trip.getTripKey(),
            trip.getUser().getUserId(),
            mediaFileId,
            actorId,
            actorNickname,
            isOwner));
  }

  @Transactional
  public MediaProcessingResponse requestProcessing(User user, String tripKey, MediaProcessingRequest request) {
    Trip trip = tripAccessGuard.validateAccessibleTripByKey(tripKey, user);

    List<Long> ids = request.items().stream().map(MediaProcessingRequest.Item::mediaFileId).toList();
    Map<Long, MediaFile> byId = mediaFileRepository.findAllById(ids).stream()
            .collect(Collectors.toMap(MediaFile::getMediaFileId, mf -> mf));

    if (byId.size() != ids.size()) {
      throw new CustomException(ResultCode.MEDIA_FILE_NOT_FOUND);
    }

    byId.values().forEach(mf -> {
      if (!mf.getTrip().getTripId().equals(trip.getTripId())) {
        throw new CustomException(ResultCode.ACCESS_DENIED);
      }
    });

    List<PinPoint> existingPinPoints = pinPointService.findAllByTripId(trip.getTripId());

    List<MediaProcessingResponse.Item> items = request.items().stream()
            .map(reqItem -> {
              MediaFile mf = byId.get(reqItem.mediaFileId());

              LocalDateTime recordDate = DateFormatter.convertToLocalDateTime(reqItem.recordDate());
              PinPoint pinPoint = pinPointService.assignPinPoint(
                      existingPinPoints, trip, reqItem.latitude(), reqItem.longitude());
              mf.updateRecordDate(recordDate);
              mf.updateLocation(reqItem.latitude(), reqItem.longitude(), pinPoint);

              String currentUrl = s3KeyResolver.buildUrl(mf.getTempKey());
              mf.updateMediaLink(currentUrl);

              mf.markUploaded();

              eventPublisher.publishEvent(
                      MediaFileRegisteredEvent.v2(mf.getMediaFileId(), mf.getTempKey(), mf.getFinalKey()));

              String finalUrl = s3KeyResolver.buildUrl(mf.getFinalKey());
              return new MediaProcessingResponse.Item(
                      mf.getMediaFileId(), currentUrl, finalUrl, mf.getProcessingStatus().name());
            })
            .toList();

    return new MediaProcessingResponse(items);
  }

}