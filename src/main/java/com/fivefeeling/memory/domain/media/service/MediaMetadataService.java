package com.fivefeeling.memory.domain.media.service;

import com.fivefeeling.memory.domain.media.dto.MediaFileBatchDeleteRequestDTO;
import com.fivefeeling.memory.domain.media.dto.MediaFileRequestDTO;
import com.fivefeeling.memory.domain.media.dto.MediaFileUpdateRequestDTO;
import com.fivefeeling.memory.domain.media.model.MediaFile;
import com.fivefeeling.memory.domain.media.repository.MediaFileRepository;
import com.fivefeeling.memory.domain.pinpoint.model.PinPoint;
import com.fivefeeling.memory.domain.pinpoint.service.PinPointService;
import com.fivefeeling.memory.domain.trip.model.Trip;
import com.fivefeeling.memory.domain.trip.repository.TripRepository;
import com.fivefeeling.memory.global.common.ResultCode;
import com.fivefeeling.memory.global.exception.CustomException;
import com.fivefeeling.memory.global.s3.S3UploadService;
import com.fivefeeling.memory.global.util.DateUtil;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
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


  @Value("${spring.cloud.aws.s3.bucketName}")
  private String bucketName;

  public void processAndSaveMetadataBatch(Long tripId, List<MediaFileRequestDTO> files) {
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
                      .mediaType(file.mediaType())
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
}
