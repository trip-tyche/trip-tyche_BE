package com.triptyche.backend.domain.media.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.triptyche.backend.domain.media.dto.MediaFileBatchDeleteRequestDTO;
import com.triptyche.backend.domain.media.dto.MediaFileBatchUpdateRequestDTO;
import com.triptyche.backend.domain.media.dto.UnlocatedImageResponseDTO;
import com.triptyche.backend.domain.media.dto.UnlocatedImageResponseDTO.Media;
import com.triptyche.backend.domain.media.dto.UpdateMediaFileInfoRequestDTO;
import com.triptyche.backend.domain.media.dto.UpdateMediaFileLocationRequestDTO;
import com.triptyche.backend.domain.media.event.MediaFileAddedEvent;
import com.triptyche.backend.domain.media.event.MediaFileDeletedEvent;
import com.triptyche.backend.domain.media.event.MediaFileLocationUpdatedEvent;
import com.triptyche.backend.domain.media.event.MediaFileUpdatedEvent;
import com.triptyche.backend.domain.media.model.MediaFile;
import com.triptyche.backend.domain.media.repository.MediaFileRepository;
import com.triptyche.backend.domain.trip.model.PinPoint;
import com.triptyche.backend.domain.trip.repository.PinPointRepository;
import com.triptyche.backend.domain.trip.service.PinPointService;
import com.triptyche.backend.domain.trip.model.Trip;
import com.triptyche.backend.domain.trip.validator.TripAccessValidator;
import com.triptyche.backend.domain.user.model.User;
import com.triptyche.backend.global.common.ResultCode;
import com.triptyche.backend.global.exception.CustomException;
import com.triptyche.backend.global.redis.ImageQueueService;
import com.triptyche.backend.global.s3.S3UploadService;
import com.triptyche.backend.global.util.DateFormatter;
import com.triptyche.backend.global.util.DateUtil;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MediaMetadataService {

  private final MediaFileRepository mediaFileRepository;
  private final PinPointService pinPointService;
  private final PinPointRepository pinPointRepository;
  private final RedisDataService redisDataService;
  private final S3UploadService s3UploadService;
  private final ImageQueueService imageQueueService;
  private final TripAccessValidator tripAccessValidator;
  private final ApplicationEventPublisher eventPublisher;
  private final ObjectMapper objectMapper;


  @Value("${spring.cloud.aws.s3.bucketName}")
  private String bucketName;

  public void processAndSaveMetadataBatch(User user, String tripKey, List<UpdateMediaFileInfoRequestDTO> files) {
    Trip trip = tripAccessValidator.validateAccessibleTripByKey(tripKey, user);

    boolean isOwner = trip.getUser().getUserId().equals(user.getUserId());

    List<PinPoint> existingPinPoints = new ArrayList<>(
            pinPointRepository.findAllByTripTripId(trip.getTripId()));

    List<MediaFile> mediaFiles = files.stream()
            .map(file -> {
              LocalDateTime recordDateTime = DateUtil.convertToLocalDateTime(file.recordDate());
              PinPoint pinPoint = pinPointService.findOrCreateFromList(
                      existingPinPoints, trip, file.latitude(), file.longitude());
              String mediaKey = extractMediaKey(file.mediaLink());
              return MediaFile.builder()
                      .trip(trip)
                      .pinPoint(pinPoint)
                      .mediaType("image/webp")
                      .mediaLink(file.mediaLink())
                      .mediaKey(mediaKey)
                      .recordDate(recordDateTime)
                      .latitude(file.latitude())
                      .longitude(file.longitude())
                      .build();
            })
            .collect(Collectors.toList());
    List<MediaFile> savedMediaFiles = mediaFileRepository.saveAll(mediaFiles);

    // Redis 처리 (위치 0.0인 파일들만)
    savedMediaFiles.stream()
            .filter(mf -> mf.getLatitude() == 0.0 && mf.getLongitude() == 0.0)
            .forEach(mf -> {
              redisDataService.saveZeroLocationData(
                      trip.getTripId(),
                      mf.getMediaFileId(),
                      mf.getMediaLink(),
                      mf.getRecordDate().toString()
              );
            });

    eventPublisher.publishEvent(new MediaFileAddedEvent(
            trip,
            user.getUserId(),
            user.getUserNickName(),
            isOwner,
            savedMediaFiles.size()));
  }

  @Transactional
  public int updateMultipleMediaFiles(User user, String tripKey, MediaFileBatchUpdateRequestDTO requestDTO) {
    Trip trip = tripAccessValidator.validateAccessibleTripByKey(tripKey, user);
    Long actorId = user.getUserId();
    String actorNickname = user.getUserNickName();
    boolean isOwner = trip.getUser().getUserId().equals(actorId);

    List<PinPoint> existingPinPoints = new ArrayList<>(
            pinPointRepository.findAllByTripTripId(trip.getTripId()));

    List<MediaFile> updatedMediaFiles = requestDTO.mediaFiles().stream()
            .map(request -> {
              MediaFile mf = mediaFileRepository.findById(request.mediaFileId())
                      .orElseThrow(() -> new CustomException(ResultCode.MEDIA_FILE_NOT_FOUND));

              PinPoint pinPoint = pinPointService.findOrCreateFromList(
                      existingPinPoints, trip, request.latitude(), request.longitude());

              mf.updateRecordDate(request.recordDate());
              mf.updateLocation(request.latitude(), request.longitude(), pinPoint);
              return mediaFileRepository.save(mf);
            })
            .toList();

    eventPublisher.publishEvent(new MediaFileUpdatedEvent(
            trip,
            actorId,
            actorNickname,
            isOwner,
            updatedMediaFiles.size()
    ));
    return updatedMediaFiles.size();
  }

  @Transactional
  public int deleteMultipleMediaFiles(User user, String tripKey, MediaFileBatchDeleteRequestDTO requestDTO) {
    Trip trip = tripAccessValidator.validateAccessibleTripByKey(tripKey, user);
    Long actorId = user.getUserId();
    String actorNickname = user.getUserNickName();
    boolean isOwner = trip.getUser().getUserId().equals(actorId);

    List<MediaFile> mediaFiles = mediaFileRepository.findAllById(requestDTO.mediaFileIds());

    List<String> mediaKeys = mediaFiles.stream()
            .map(MediaFile::getMediaKey)
            .toList();

    s3UploadService.deleteFiles(mediaKeys);
    mediaFileRepository.deleteAll(mediaFiles);

    eventPublisher.publishEvent(new MediaFileDeletedEvent(
            trip,
            actorId,
            actorNickname,
            isOwner,
            mediaFiles.size()
    ));

    return mediaFiles.size();
  }

  @Transactional(readOnly = true)
  public List<UnlocatedImageResponseDTO> getUnlocatedImages(User user, String tripKey) {
    Trip trip = tripAccessValidator.validateAccessibleTripByKey(tripKey, user);
    String redisKey = "trip:" + trip.getTripId();
    Map<Object, Object> redisData = imageQueueService.getImageQueue(redisKey);

    if (redisData.isEmpty()) {
      throw new CustomException(ResultCode.EDIT_DATA_NOT_FOUND);
    }

    Map<String, List<Media>> groupedByDate = redisData.entrySet().stream()
            .map(entry -> {
              try {
                Long mediaFileId = Long.valueOf(entry.getKey().toString());
                Map<String, Object> imageData = objectMapper.readValue(entry.getValue().toString(), Map.class);
                String mediaLink = (String) imageData.get("mediaLink");
                String recordDateString = (String) imageData.get("recordDate");
                LocalDateTime recordDateTime = LocalDateTime.parse(recordDateString);
                String formattedDate = DateFormatter.formatLocalDateToString(recordDateTime.toLocalDate());

                return Map.entry(formattedDate, new Media(mediaFileId, mediaLink));
              } catch (Exception e) {
                throw new RuntimeException("Json 파싱 오류", e);
              }
            })
            .collect(Collectors.groupingBy(
                    Entry::getKey,
                    Collectors.mapping(Entry::getValue, Collectors.toList())
            ));

    return groupedByDate.entrySet().stream()
            .sorted(Entry.comparingByKey())
            .map(entry -> new UnlocatedImageResponseDTO(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());
  }

  @Transactional
  public void updateImageLocation(User user,
                                  String tripKey,
                                  Long mediaFileId,
                                  UpdateMediaFileLocationRequestDTO requestDTO) {

    Double newLat = requestDTO.latitude();
    Double newLon = requestDTO.longitude();
    if (newLat == null || newLon == null) {
      throw new CustomException(ResultCode.INVALID_COORDINATE);
    }

    Trip trip = tripAccessValidator.validateAccessibleTripByKey(tripKey, user);
    MediaFile mf = mediaFileRepository.findById(mediaFileId)
            .orElseThrow(() -> new CustomException(ResultCode.MEDIA_FILE_NOT_FOUND));

    PinPoint pinPoint = pinPointService.findOrCreatePinPoint(trip, newLat, newLon);
    mf.updateLocation(newLat, newLon, pinPoint);
    mediaFileRepository.save(mf);

    // Redis 캐시 삭제
    String redisKey = "trip:" + trip.getTripId();
    imageQueueService.deleteFromImageQueue(redisKey, String.valueOf(mediaFileId));

    // 이벤트 발행: 위치 갱신
    Long actorId = user.getUserId();
    String actorNickname = user.getUserNickName();
    boolean isOwner = trip.getUser().getUserId().equals(actorId);
    eventPublisher.publishEvent(new MediaFileLocationUpdatedEvent(trip, mediaFileId, actorId, actorNickname, isOwner));
  }

  private String extractMediaKey(String mediaLink) {
    URI uri = URI.create(mediaLink);
    return uri.getPath().substring(1);
  }
}