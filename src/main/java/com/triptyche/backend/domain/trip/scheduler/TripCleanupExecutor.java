package com.triptyche.backend.domain.trip.scheduler;

import com.triptyche.backend.domain.media.repository.MediaFileRepository;
import com.triptyche.backend.domain.share.repository.ShareRepository;
import com.triptyche.backend.domain.trip.model.Trip;
import com.triptyche.backend.domain.trip.repository.PinPointRepository;
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
  private final PinPointRepository pinPointRepository;

  @Transactional
  public void deleteAbandonedTrips(List<Trip> trips) {
    mediaFileRepository.deleteAllByTripIn(trips);
    pinPointRepository.deleteAllByTripIn(trips);
    tripRepository.deleteAllIn(trips);
    log.info("방치된 여행 삭제 완료 — {}건", trips.size());
  }

  @Transactional
  public void deleteSoftDeletedTrips(List<Trip> trips) {
    shareRepository.deleteAllByTripIn(trips);
    mediaFileRepository.deleteAllByTripIn(trips);
    pinPointRepository.deleteAllByTripIn(trips);
    tripRepository.deleteAllIn(trips);
    log.info("만료된 여행 영구 삭제 완료 — {}건", trips.size());
  }
}