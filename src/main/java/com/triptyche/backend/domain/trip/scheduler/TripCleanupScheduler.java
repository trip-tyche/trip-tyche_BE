package com.triptyche.backend.domain.trip.scheduler;

import com.triptyche.backend.domain.media.model.MediaFile;
import com.triptyche.backend.domain.media.repository.MediaFileRepository;
import com.triptyche.backend.domain.share.repository.ShareRepository;
import com.triptyche.backend.domain.trip.model.Trip;
import com.triptyche.backend.domain.trip.model.TripStatus;
import com.triptyche.backend.domain.trip.repository.TripRepository;
import com.triptyche.backend.global.s3.S3UploadService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TripCleanupScheduler {

  private final TripRepository tripRepository;
  private final ShareRepository shareRepository;
  private final MediaFileRepository mediaFileRepository;
  private final S3UploadService s3UploadService;

  @Value("${trip.soft-delete.retention-days:30}")
  private int retentionDays;

  @Scheduled(cron = "0 0 * * * *")
  @Transactional
  public void cleanupDraftTrips() {
    log.info("🧹 TripCleanupScheduler: 예약된 임시 여행 삭제 작업 시작");
    List<Trip> draftTrips = tripRepository.findByStatus(TripStatus.DRAFT);
    log.info("삭제 대상 DRAFT 여행 개수: {}", draftTrips.size());

    if (!draftTrips.isEmpty()) {
      tripRepository.deleteAll(draftTrips);
      log.info("총 {}개의 임시 여행을 삭제했습니다.", draftTrips.size());
    } else {
      log.info("삭제할 임시 여행이 없습니다.");
    }
  }

  @Scheduled(cron = "0 0 3 * * *")
  @Transactional
  public void cleanupSoftDeletedTrips() {
    LocalDateTime threshold = LocalDateTime.now().minusDays(retentionDays);
    List<Trip> deletedTrips = tripRepository.findSoftDeletedBefore(threshold);

    if (deletedTrips.isEmpty()) {
      log.info("정리할 여행 없음");
      return;
    }

    log.info("DB 정리 시작 — 대상: {}건", deletedTrips.size());
    shareRepository.deleteAllByTripIn(deletedTrips);
    mediaFileRepository.deleteAll(mediaFileRepository.findAllByTripIn(deletedTrips));
    tripRepository.deleteAll(deletedTrips);
    log.info("DB 정리 완료 — {}건", deletedTrips.size());
  }

  @Scheduled(cron = "0 5 3 * * *")
  public void cleanupSoftDeletedStorage() {
    LocalDateTime threshold = LocalDateTime.now().minusDays(retentionDays);
    List<MediaFile> orphanedFiles =
        mediaFileRepository.findByTripDeletedAtBefore(threshold);

    if (orphanedFiles.isEmpty()) {
      log.info("정리할 S3 파일 없음");
      return;
    }

    List<String> mediaKeys = orphanedFiles.stream()
            .map(MediaFile::getMediaKey)
            .toList();

    s3UploadService.deleteFiles(mediaKeys);
    log.info("S3 파일 정리 완료 — {}건", mediaKeys.size());
  }
}