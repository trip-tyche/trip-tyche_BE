package com.triptyche.backend.domain.trip.scheduler;

import com.triptyche.backend.domain.media.model.MediaFile;
import com.triptyche.backend.domain.media.repository.MediaFileRepository;
import com.triptyche.backend.domain.trip.model.Trip;
import com.triptyche.backend.domain.trip.repository.TripRepository;
import com.triptyche.backend.global.s3.S3KeyResolver;
import com.triptyche.backend.global.s3.S3UploadService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TripCleanupScheduler {

  private final TripRepository tripRepository;
  private final MediaFileRepository mediaFileRepository;
  private final TripCleanupExecutor cleanupExecutor;
  private final S3UploadService s3UploadService;

  @Value("${trip.soft-delete.retention-days:30}")
  private int retentionDays;

  private static final int ABANDONED_TRIP_EXPIRY_HOURS = 24;

  @Scheduled(cron = "0 0 * * * *")
  public void cleanupAbandonedTrips() {
    LocalDateTime threshold = LocalDateTime.now().minusHours(ABANDONED_TRIP_EXPIRY_HOURS);
    List<Trip> abandonedTrips = tripRepository.findAbandonedTripsBefore(threshold);

    if (abandonedTrips.isEmpty()) {
      log.debug("삭제할 방치된 여행이 없습니다.");
      return;
    }

    List<String> mediaKeys = collectAllKeys(mediaFileRepository.findAllByTripIn(abandonedTrips));

    cleanupExecutor.deleteAbandonedTrips(abandonedTrips);
    deleteFromStorage(mediaKeys);
  }

  @Scheduled(cron = "0 0 3 * * *")
  public void cleanupSoftDeletedTrips() {
    LocalDateTime threshold = LocalDateTime.now().minusDays(retentionDays);
    List<Trip> deletedTrips = tripRepository.findSoftDeletedBefore(threshold);

    if (deletedTrips.isEmpty()) {
      log.debug("만료된 여행 없음 — 정리 생략");
      return;
    }

    List<String> mediaKeys = collectAllKeys(mediaFileRepository.findAllByTripIn(deletedTrips));

    cleanupExecutor.deleteSoftDeletedTrips(deletedTrips);
    deleteFromStorage(mediaKeys);
  }

  private List<String> collectAllKeys(List<MediaFile> mediaFiles) {
    return mediaFiles.stream()
            .flatMap(mf -> {
              List<String> keys = new ArrayList<>();
              if (mf.getMediaKey() != null) {
                keys.add(mf.getMediaKey());
              }
              String webpKey = s3UploadService.extractKey(mf.getMediaLink());
              if (webpKey != null && !webpKey.equals(mf.getMediaKey())) {
                keys.add(webpKey);
              }
              return keys.stream();
            })
            .distinct()
            .toList();
  }

  private void deleteFromStorage(List<String> mediaKeys) {
    if (mediaKeys.isEmpty()) return;

    List<String> deletableKeys = mediaKeys.stream()
            .filter(key -> !S3KeyResolver.isSeedKey(key))
            .toList();

    if (deletableKeys.isEmpty()) return;

    try {
      s3UploadService.deleteFiles(deletableKeys);
      log.info("S3 파일 정리 완료 — {}건", deletableKeys.size());
    } catch (Exception e) {
      log.error("S3 파일 정리 실패 — {}건, 수동 확인 필요", deletableKeys.size(), e);
    }
  }
}