package com.triptyche.backend.domain.trip.scheduler;

import com.triptyche.backend.domain.media.model.MediaFile;
import com.triptyche.backend.domain.media.repository.MediaFileRepository;
import com.triptyche.backend.domain.share.repository.ShareRepository;
import com.triptyche.backend.domain.trip.model.Trip;
import com.triptyche.backend.domain.trip.repository.TripRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TripCleanupExecutor {

  private final TripRepository tripRepository;
  private final ShareRepository shareRepository;
  private final MediaFileRepository mediaFileRepository;

  @Transactional
  public void deleteAbandonedTrips(List<Trip> trips) {
    tripRepository.deleteAll(trips);
    log.info("방치된 여행 DB 삭제 완료 — {}건", trips.size());
  }

  @Transactional
  public void deleteSoftDeletedTrips(List<Trip> trips, List<MediaFile> mediaFiles) {
    shareRepository.deleteAllByTripIn(trips);
    mediaFileRepository.deleteAll(mediaFiles);
    tripRepository.deleteAll(trips);
    log.info("소프트 삭제 여행 DB 정리 완료 — {}건", trips.size());
  }
}