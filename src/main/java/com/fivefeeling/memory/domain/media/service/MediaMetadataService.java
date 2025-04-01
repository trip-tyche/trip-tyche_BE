package com.fivefeeling.memory.domain.media.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fivefeeling.memory.domain.media.dto.MediaFileBatchDeleteRequestDTO;
import com.fivefeeling.memory.domain.media.dto.MediaFileBatchUpdateRequestDTO;
import com.fivefeeling.memory.domain.media.dto.MediaFileUpdateRequestDTO;
import com.fivefeeling.memory.domain.media.dto.UnlocatedImageResponseDTO;
import com.fivefeeling.memory.domain.media.dto.UpdateMediaFileInfoRequestDTO;
import com.fivefeeling.memory.domain.media.dto.UpdateMediaFileLocationRequestDTO;
import com.fivefeeling.memory.domain.media.model.MediaFile;
import com.fivefeeling.memory.domain.media.repository.MediaFileRepository;
import com.fivefeeling.memory.domain.pinpoint.model.PinPoint;
import com.fivefeeling.memory.domain.pinpoint.service.PinPointService;
import com.fivefeeling.memory.domain.trip.model.Trip;
import com.fivefeeling.memory.domain.trip.repository.TripRepository;
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


  @Value("${spring.cloud.aws.s3.bucketName}")
  private String bucketName;

  // ✅
  public void processAndSaveMetadataBatch(Long tripId, List<UpdateMediaFileInfoRequestDTO> files) {
    Trip trip = tripRepository.findById(tripId)
            .orElseThrow(() -> new CustomException(ResultCode.TRIP_NOT_FOUND));

    List<MediaFile> mediaFiles = files.stream()
            .map(file -> {
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

    List<MediaFile> saveMediaFiles = mediaFileRepository.saveAll(mediaFiles);

    saveMediaFiles.forEach(savedMediaFile -> {
      Long mediaFileId = savedMediaFile.getMediaFileId();

      // Redis에 저장
      if (savedMediaFile.getLatitude() == 0.0 && savedMediaFile.getLongitude() == 0.0) {
        redisDataService.saveZeroLocationData(
                tripId,
                mediaFileId,
                savedMediaFile.getMediaLink(),
                savedMediaFile.getRecordDate().toString()
        );
      }
    });
  }

  @Transactional
  public void updateMediaFileMetadata(Long tripId, Long mediaFileId, MediaFileUpdateRequestDTO request) {

    Trip trip = tripRepository.findById(tripId)
            .orElseThrow(() -> new CustomException(ResultCode.TRIP_NOT_FOUND));

    MediaFile mediaFile = mediaFileRepository.findById(mediaFileId)
            .orElseThrow(() -> new CustomException(ResultCode.MEDIA_FILE_NOT_FOUND));

    PinPoint pinPoint = pinPointService.findOrCreatePinPoint(trip, request.latitude(), request.longitude());

    mediaFile.setRecordDate(request.recordDate());
    mediaFile.setLatitude(request.latitude());
    mediaFile.setLongitude(request.longitude());
    mediaFile.setPinPoint(pinPoint);

    mediaFileRepository.save(mediaFile);
  }

  @Transactional
  public int updateMultipleMediaFiles(Long tripId, MediaFileBatchUpdateRequestDTO requestDTO) {
    Trip trip = tripRepository.findById(tripId)
            .orElseThrow(() -> new CustomException(ResultCode.TRIP_NOT_FOUND));

    List<MediaFile> updatedMediaFiles = requestDTO.mediaFiles().stream()
            .map(request -> {
              MediaFile mediaFile = mediaFileRepository.findById(request.mediaFileId())
                      .orElseThrow(() -> new CustomException(ResultCode.MEDIA_FILE_NOT_FOUND));

              PinPoint pinPoint = pinPointService.findOrCreatePinPoint(trip, request.latitude(),
                      request.longitude());

              mediaFile.setRecordDate(request.recordDate());
              mediaFile.setLatitude(request.latitude());
              mediaFile.setLongitude(request.longitude());
              mediaFile.setPinPoint(pinPoint);

              return mediaFile;
            })
            .toList();
    mediaFileRepository.saveAll(updatedMediaFiles);

    return updatedMediaFiles.size();
  }

  @Transactional
  public void deleteSingleMediaFile(Long tripId, Long mediaFileId) {
    tripRepository.findById(tripId)
            .orElseThrow(() -> new CustomException(ResultCode.TRIP_NOT_FOUND));

    MediaFile mediaFile = mediaFileRepository.findById(mediaFileId)
            .orElseThrow(() -> new CustomException(ResultCode.MEDIA_FILE_NOT_FOUND));

    s3UploadService.deleteFiles(List.of(mediaFile.getMediaKey()));

    mediaFileRepository.delete(mediaFile);
  }

  @Transactional
  public int deleteMultipleMediaFiles(Long tripId, MediaFileBatchDeleteRequestDTO requestDTO) {
    tripRepository.findById(tripId)
            .orElseThrow(() -> new CustomException(ResultCode.TRIP_NOT_FOUND));

    List<Long> mediaFileIds = requestDTO.mediaFileIds();

    List<MediaFile> mediaFiles = mediaFileRepository.findAllById(mediaFileIds);

    List<String> mediaKeys = mediaFiles.stream()
            .map(MediaFile::getMediaKey)
            .toList();

    s3UploadService.deleteFiles(mediaKeys);

    mediaFileRepository.deleteAll(mediaFiles);

    return mediaFiles.size();
  }

  private String extractMediaKey(String mediaLink) {
    URI uri = URI.create(mediaLink);
    return uri.getPath().substring(1);
  }

  @Transactional(readOnly = true)
  public List<UnlocatedImageResponseDTO> getUnlocatedImages(Long tripId) {
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
  public void updateImageLocation(Long tripId, Long mediaFileId, UpdateMediaFileLocationRequestDTO requestDTO) {
    // 직접 요청 DTO에서 위도와 경도 값을 추출
    Double newLatitude = requestDTO.latitude();
    Double newLongitude = requestDTO.longitude();

    if (newLatitude == null || newLongitude == null) {
      throw new CustomException(ResultCode.INVALID_COORDINATE);
    }

    // Trip 조회
    Trip trip = tripRepository.findById(tripId)
            .orElseThrow(() -> new CustomException(ResultCode.TRIP_NOT_FOUND));

    // MediaFile 조회
    MediaFile mediaFile = mediaFileRepository.findById(mediaFileId)
            .orElseThrow(() -> new CustomException(ResultCode.MEDIA_FILE_NOT_FOUND));

    // PinPoint 조회 또는 생성
    PinPoint pinPoint = pinPointService.findOrCreatePinPoint(trip, newLatitude, newLongitude);

    // MediaFile의 위치정보 업데이트
    mediaFile.setLatitude(newLatitude);
    mediaFile.setLongitude(newLongitude);
    mediaFile.setPinPoint(pinPoint);
    mediaFileRepository.save(mediaFile);

    // Redis 캐시에서 해당 미디어파일 삭제
    String redisKey = "trip:" + tripId;
    imageQueueService.deleteFromImageQueue(redisKey, String.valueOf(mediaFileId));
  }
}
