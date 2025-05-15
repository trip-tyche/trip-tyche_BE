package com.fivefeeling.memory.domain.media.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fivefeeling.memory.domain.media.dto.MediaFileBatchDeleteRequestDTO;
import com.fivefeeling.memory.domain.media.dto.MediaFileBatchUpdateRequestDTO;
import com.fivefeeling.memory.domain.media.dto.UnlocatedImageResponseDTO;
import com.fivefeeling.memory.domain.media.dto.UpdateMediaFileInfoRequestDTO;
import com.fivefeeling.memory.domain.media.dto.UpdateMediaFileLocationRequestDTO;
import com.fivefeeling.memory.domain.media.event.MediaFileAddedEvent;
import com.fivefeeling.memory.domain.media.event.MediaFileDateUpdatedEvent;
import com.fivefeeling.memory.domain.media.event.MediaFileDeletedEvent;
import com.fivefeeling.memory.domain.media.event.MediaFileLocationUpdatedEvent;
import com.fivefeeling.memory.domain.media.model.MediaFile;
import com.fivefeeling.memory.domain.media.repository.MediaFileRepository;
import com.fivefeeling.memory.domain.pinpoint.model.PinPoint;
import com.fivefeeling.memory.domain.pinpoint.service.PinPointService;
import com.fivefeeling.memory.domain.trip.model.Trip;
import com.fivefeeling.memory.domain.trip.repository.TripRepository;
import com.fivefeeling.memory.domain.trip.validator.TripAccessValidator;
import com.fivefeeling.memory.global.common.ResultCode;
import com.fivefeeling.memory.global.exception.CustomException;
import com.fivefeeling.memory.global.redis.ImageQueueService;
import com.fivefeeling.memory.global.s3.S3UploadService;
import com.fivefeeling.memory.global.util.DateFormatter;
import com.fivefeeling.memory.global.util.DateUtil;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MediaMetadataService {

  private final TripRepository tripRepository;
  private final MediaFileRepository mediaFileRepository;
  private final PinPointService pinPointService;
  private final RedisDataService redisDataService;
  private final S3UploadService s3UploadService;
  private final ImageQueueService imageQueueService;
  private final TripAccessValidator tripAccessValidator;
  private final ApplicationEventPublisher eventPublisher;


  @Value("${spring.cloud.aws.s3.bucketName}")
  private String bucketName;

  // ✅
  public void processAndSaveMetadataBatch(Long tripId, List<UpdateMediaFileInfoRequestDTO> files) {
    Trip trip = tripRepository.findById(tripId)
            .orElseThrow(() -> new CustomException(ResultCode.TRIP_NOT_FOUND));

    // 엔티티 생성 및 배치 저장
    List<MediaFile> mediaFiles = files.stream()
            .map(file -> {
              // DTO 필드 매핑
              LocalDateTime recordDateTime = DateUtil.convertToLocalDateTime(file.recordDate());
              PinPoint pinPoint = pinPointService.findOrCreatePinPoint(trip, file.latitude(), file.longitude());
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

    // 저장 후 Redis 및 이벤트 발행 처리
    savedMediaFiles.forEach(mf -> {
      Long mediaId = mf.getMediaFileId();
      // Redis 저장: 위치 0.0인 경우
      if (mf.getLatitude() == 0.0 && mf.getLongitude() == 0.0) {
        redisDataService.saveZeroLocationData(
                tripId,
                mediaId,
                mf.getMediaLink(),
                mf.getRecordDate().toString()
        );
      }
      // 이벤트 발행
      eventPublisher.publishEvent(new MediaFileAddedEvent(trip, mediaId));
    });
  }

//  @Transactional
//  public void updateMediaFileMetadata(String userEmail, Long tripId, Long mediaFileId,
//                                      MediaFileUpdateRequestDTO request) {
//    Trip trip = tripAccessValidator.validateAccessibleTrip(tripId, userEmail);
//
//    MediaFile mediaFile = mediaFileRepository.findById(mediaFileId)
//            .orElseThrow(() -> new CustomException(ResultCode.MEDIA_FILE_NOT_FOUND));
//
//    PinPoint pinPoint = pinPointService.findOrCreatePinPoint(trip, request.latitude(), request.longitude());
//
//    mediaFile.setRecordDate(request.recordDate());
//    mediaFile.setLatitude(request.latitude());
//    mediaFile.setLongitude(request.longitude());
//    mediaFile.setPinPoint(pinPoint);
//
//    mediaFileRepository.save(mediaFile);
//  }

  @Transactional
  public int updateMultipleMediaFiles(String userEmail, Long tripId, MediaFileBatchUpdateRequestDTO requestDTO) {
    Trip trip = tripAccessValidator.validateAccessibleTrip(tripId, userEmail);

    List<MediaFile> updatedMediaFiles = requestDTO.mediaFiles().stream()
            .map(request -> {
              MediaFile mf = mediaFileRepository.findById(request.mediaFileId())
                      .orElseThrow(() -> new CustomException(ResultCode.MEDIA_FILE_NOT_FOUND));

              PinPoint pinPoint = pinPointService.findOrCreatePinPoint(trip, request.latitude(),
                      request.longitude());

              mf.setRecordDate(request.recordDate());
              mf.setLatitude(request.latitude());
              mf.setLongitude(request.longitude());
              mf.setPinPoint(pinPoint);
              return mediaFileRepository.save(mf);
            })
            .toList();

    updatedMediaFiles.forEach(mf ->
            eventPublisher.publishEvent(new MediaFileDateUpdatedEvent(trip, mf.getMediaFileId()))
    );

    return updatedMediaFiles.size();
  }

//  @Transactional
//  public void deleteSingleMediaFile(String userEmail, Long tripId, Long mediaFileId) {
//    tripAccessValidator.validateAccessibleTrip(tripId, userEmail);
//
//    MediaFile mediaFile = mediaFileRepository.findById(mediaFileId)
//            .orElseThrow(() -> new CustomException(ResultCode.MEDIA_FILE_NOT_FOUND));
//
//    s3UploadService.deleteFiles(List.of(mediaFile.getMediaKey()));
//
//    mediaFileRepository.delete(mediaFile);
//  }

  @Transactional
  public int deleteMultipleMediaFiles(String userEmail, Long tripId, MediaFileBatchDeleteRequestDTO requestDTO) {
    Trip trip = tripAccessValidator.validateAccessibleTrip(tripId, userEmail);

    List<MediaFile> mediaFiles = mediaFileRepository.findAllById(requestDTO.mediaFileIds());
    List<String> mediaKeys = mediaFiles.stream()
            .map(MediaFile::getMediaKey)
            .toList();

    s3UploadService.deleteFiles(mediaKeys);
    mediaFileRepository.deleteAll(mediaFiles);

    mediaFiles.forEach(mf ->
            eventPublisher.publishEvent(new MediaFileDeletedEvent(trip, mf.getMediaFileId()))
    );

    return mediaFiles.size();
  }
  
  @Transactional(readOnly = true)
  public List<UnlocatedImageResponseDTO> getUnlocatedImages(String userEmail, Long tripId) {
    tripAccessValidator.validateAccessibleTrip(tripId, userEmail);
    String redisKey = "trip:" + tripId;
    Map<Object, Object> redisData = imageQueueService.getImageQueue(redisKey);

    if (redisData.isEmpty()) {
      throw new CustomException(ResultCode.EDIT_DATA_NOT_FOUND);
    }

    ObjectMapper objectMapper = new ObjectMapper();

    Map<String, List<UnlocatedImageResponseDTO.Media>> groupedByDate = redisData.entrySet().stream()
            .map(entry -> {
              try {
                Long mediaFileId = Long.valueOf(entry.getKey().toString());
                Map<String, Object> imageData = objectMapper.readValue(entry.getValue().toString(), Map.class);
                String mediaLink = (String) imageData.get("mediaLink");
                String recordDateString = (String) imageData.get("recordDate");
                LocalDateTime recordDateTime = LocalDateTime.parse(recordDateString);
                String formattedDate = DateFormatter.formatLocalDateToString(recordDateTime.toLocalDate());

                return Map.entry(formattedDate, new UnlocatedImageResponseDTO.Media(mediaFileId, mediaLink));
              } catch (Exception e) {
                throw new RuntimeException("Json 파싱 오류", e);
              }
            })
            .collect(Collectors.groupingBy(
                    Map.Entry::getKey,
                    Collectors.mapping(Map.Entry::getValue, Collectors.toList())
            ));

    return groupedByDate.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> new UnlocatedImageResponseDTO(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());
  }

  @Transactional
  public void updateImageLocation(String userEmail, Long tripId, Long mediaFileId,
                                  UpdateMediaFileLocationRequestDTO requestDTO) {

    Double newLat = requestDTO.latitude();
    Double newLon = requestDTO.longitude();
    if (newLat == null || newLon == null) {
      throw new CustomException(ResultCode.INVALID_COORDINATE);
    }

    Trip trip = tripAccessValidator.validateAccessibleTrip(tripId, userEmail);
    MediaFile mf = mediaFileRepository.findById(mediaFileId)
            .orElseThrow(() -> new CustomException(ResultCode.MEDIA_FILE_NOT_FOUND));

    PinPoint pinPoint = pinPointService.findOrCreatePinPoint(trip, newLat, newLon);
    mf.setLatitude(newLat);
    mf.setLongitude(newLon);
    mf.setPinPoint(pinPoint);
    mediaFileRepository.save(mf);

    // Redis 캐시 삭제
    String redisKey = "trip:" + tripId;
    imageQueueService.deleteFromImageQueue(redisKey, String.valueOf(mediaFileId));

    // 이벤트 발행: 위치 갱신
    eventPublisher.publishEvent(new MediaFileLocationUpdatedEvent(trip, mediaFileId));
  }

  private String extractMediaKey(String mediaLink) {
    URI uri = URI.create(mediaLink);
    return uri.getPath().substring(1);
  }
}
