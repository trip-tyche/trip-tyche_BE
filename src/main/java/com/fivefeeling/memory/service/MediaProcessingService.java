package com.fivefeeling.memory.service;

import com.fivefeeling.memory.dto.ImageMetadataDTO;
import com.fivefeeling.memory.entity.MediaFile;
import com.fivefeeling.memory.entity.PinPoint;
import com.fivefeeling.memory.entity.Trip;
import com.fivefeeling.memory.repository.MediaFileRepository;
import com.fivefeeling.memory.repository.TripRepository;
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
  private final TripRepository tripRepository;

  // 파일 업로드를 처리하는 메인 메서드
  public MediaFile processFileUpload(Trip trip, MultipartFile file) {
    // 1. 메타데이터 추출
    ImageMetadataDTO metadata = metadataExtractorService.extractMetadata(file);

    // 2. PinPoint 결정
    PinPoint pinPoint = pinPointService.findOrCreatePinPoint(trip, metadata.latitude(), metadata.longitude());

    // 3. S3에 파일 업로드
    String mediaLink = s3UploadService.uploadFile(file, "uploads/" + trip.getTripId());

    // 4. MediaFile을 DB에 저장
    MediaFile mediaFile = new MediaFile();
    mediaFile.setTrip(trip);
    mediaFile.setPinPoint(pinPoint);
    mediaFile.setMediaType(metadata.mediaType());
    mediaFile.setMediaLink(mediaLink);
    mediaFile.setRecordDate(metadata.date());
    mediaFile.setLatitude(metadata.latitude());
    mediaFile.setLongitude(metadata.longitude());
    mediaFileRepository.save(mediaFile);

    return mediaFile;
  }
}