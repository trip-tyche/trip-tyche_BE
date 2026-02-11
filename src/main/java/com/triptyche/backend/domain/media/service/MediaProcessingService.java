package com.triptyche.backend.domain.media.service;

import com.triptyche.backend.domain.media.model.MediaFile;
import com.triptyche.backend.domain.media.repository.MediaFileRepository;
import com.triptyche.backend.domain.trip.model.Trip;
import com.triptyche.backend.global.s3.S3UploadService;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class MediaProcessingService {

  //
//  private final Executor cpuBoundTaskExecutor;
//  private final Executor ioBoundTaskExecutor;
  private final S3UploadService s3UploadService;
  //  private final MetadataExtractorService metadataExtractorService;
//  private final PinPointService pinPointService;
  private final MediaFileRepository mediaFileRepository;

  /*@Transactional
  public CompletableFuture<List<MediaFile>> processFileUpload(Trip trip, List<MultipartFile> files) {
    // 각 파일별로 비동기 작업 수행
    List<CompletableFuture<MediaProcessResult>> futures = files.stream()
            .map(file -> processSingleFileUpload(trip, file))
            .collect(Collectors.toList());

    // 모든 파일의 비동기 작업이 완료될 때까지 기다림
    CompletableFuture<Void> allOfFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

    // 모든 작업 완료 후 결과 수집하여 반환
    return allOfFutures.thenApplyAsync(v -> {
      log.info("모든 파일의 메타데이터와 S3 업로드 완료 후 PinPoint 및 DB 저장 시작");

      // 결과 수집
      List<MediaProcessResult> results = futures.stream()
              .map(CompletableFuture::join)
              .filter(Objects::nonNull)
              .collect(Collectors.toList());

      // 각 결과에 대해 PinPoint 추출 및 MediaFile 저장
      List<MediaFile> mediaFiles = results.stream()
              .filter(result -> result != null) // 예외 발생한 파일 제외
              .map(result -> {
                // PinPoint 찾기 또는 생성
                PinPoint pinPoint = pinPointService.findOrCreatePinPoint(trip, result.metadata().latitude(), result
                        .metadata().longitude());

                // MediaFile DB 저장
                MediaFile mediaFile = new MediaFile();
                mediaFile.setTrip(trip);
                mediaFile.setMediaLink(result.mediaLink());
                mediaFile.setMediaKey(result.mediaKey());
                mediaFile.setMediaType(result.metadata().mediaType());
                LocalDateTime recordDateTime = DateUtil.convertToLocalDateTime(result.metadata().recordDate());
                mediaFile.setRecordDate(recordDateTime);
                mediaFile.setLatitude(result.metadata().latitude());
                mediaFile.setLongitude(result.metadata().longitude());
                mediaFile.setPinPoint(pinPoint);

                return mediaFile;
              })
              .collect(Collectors.toList());
      mediaFileRepository.saveAll(mediaFiles);
//      log.info("MediaFile 배치 저장 완료 스레드 {}", Thread.currentThread().getName());
      return mediaFiles;
    }, ioBoundTaskExecutor);
  }*/

/*  // 단일 파일 처리 메서드
  private CompletableFuture<MediaProcessResult> processSingleFileUpload(Trip trip, MultipartFile file) {
//    log.info("파일 '{}' 스레드 {}", file.getOriginalFilename(), Thread.currentThread().getName());

    // 병렬 작업 - 메타데이터 추출
    CompletableFuture<ImageMetadataDTO> metadataFuture = CompletableFuture.supplyAsync(() -> {
//      log.info("메타데이터 추출 시작 '{}' 스레드 {}", file.getOriginalFilename(), Thread.currentThread().getName());
      return metadataExtractorService.extractMetadata(file);
    }, cpuBoundTaskExecutor);

    // I/O 바운드 작업 - S3 업로드
    CompletableFuture<UploadResult> uploadFuture = CompletableFuture.supplyAsync(() -> {
//      log.info("S3 업로드 시작 '{}' 스레드 {}", file.getOriginalFilename(), Thread.currentThread().getName());
      return s3UploadService.uploadFile(file, "uploads/" + trip.getTripId());
    }, ioBoundTaskExecutor);

    // 메타데이터와 S3 업로드가 모두 완료되면 결과를 결합하여 반환
    return metadataFuture.thenCombine(uploadFuture, (metadata, uploadResult) -> new MediaProcessResult(
                    metadata, uploadResult.getMediaLink(), uploadResult.getMediaKey()))
            .exceptionally(ex -> {
              log.error("파일 처리 중 오류 발생: {}", ex.getMessage());
              return null;
            });
  }*/

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
