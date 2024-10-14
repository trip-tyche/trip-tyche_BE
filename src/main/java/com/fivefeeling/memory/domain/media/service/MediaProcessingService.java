package com.fivefeeling.memory.domain.media.service;

import com.fivefeeling.memory.domain.media.model.ImageMetadataDTO;
import com.fivefeeling.memory.domain.media.model.MediaFile;
import com.fivefeeling.memory.domain.media.repository.MediaFileRepository;
import com.fivefeeling.memory.domain.pinpoint.model.PinPoint;
import com.fivefeeling.memory.domain.pinpoint.service.PinPointService;
import com.fivefeeling.memory.domain.trip.model.Trip;
import com.fivefeeling.memory.global.s3.S3UploadService;
import com.fivefeeling.memory.global.s3.UploadResult;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class MediaProcessingService {

  private final S3UploadService s3UploadService;
  private final MetadataExtractorService metadataExtractorService;
  private final PinPointService pinPointService;
  private final MediaFileRepository mediaFileRepository;

  // 파일 업로드를 처리하는 메인 메서드
  public MediaFile processFileUpload(Trip trip, MultipartFile file) {
    // 1. 메타데이터 추출
    ImageMetadataDTO metadata = metadataExtractorService.extractMetadata(file);

    // 2. PinPoint 결정
    PinPoint pinPoint = pinPointService.findOrCreatePinPoint(trip, metadata.latitude(), metadata.longitude());

    // 3. S3에 파일 업로드
    UploadResult uploadResult = s3UploadService.uploadFile(file, "uploads/" + trip.getTripId());

    // 4. MediaFile을 DB에 저장
    MediaFile mediaFile = new MediaFile();
    mediaFile.setTrip(trip);
    mediaFile.setPinPoint(pinPoint);
    mediaFile.setMediaType(metadata.mediaType());
    mediaFile.setMediaLink(uploadResult.getMediaLink());
    mediaFile.setMediaKey(uploadResult.getMediaKey());
    mediaFile.setRecordDate(metadata.recordDate());
    mediaFile.setLatitude(metadata.latitude());
    mediaFile.setLongitude(metadata.longitude());
    mediaFileRepository.save(mediaFile);

    return mediaFile;
  }

  @Transactional
  public void deleteMediaFilesByTrip(Trip trip) {
    List<MediaFile> mediaFiles = mediaFileRepository.findAllByTrip(trip);
    List<String> mediaKeys = mediaFiles.stream()
        .map(MediaFile::getMediaKey)
        .collect(Collectors.toList());

    if (!mediaKeys.isEmpty()) {
      s3UploadService.deleteFiles(mediaKeys);
    }

    mediaFileRepository.deleteAll(mediaFiles);
  }
}
