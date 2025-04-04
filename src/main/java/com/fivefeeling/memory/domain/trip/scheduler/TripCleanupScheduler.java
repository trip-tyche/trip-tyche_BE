package com.fivefeeling.memory.domain.trip.scheduler;

import com.fivefeeling.memory.domain.trip.model.Trip;
import com.fivefeeling.memory.domain.trip.repository.TripRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TripCleanupScheduler {

  private final TripRepository tripRepository;

  @Scheduled(cron = "0 0 * * * *")
  @Transactional
  public void cleanupDraftTrips() {
    log.info("🧹 TripCleanupScheduler: 예약된 임시 여행 삭제 작업 시작");
    List<Trip> draftTrips = tripRepository.findByStatus("DRAFT");
    log.info("삭제 대상 DRAFT 여행 개수: {}", draftTrips.size());

    if (!draftTrips.isEmpty()) {
      tripRepository.deleteAll(draftTrips);
      log.info("총 {}개의 임시 여행을 삭제했습니다.", draftTrips.size());
    } else {
      log.info("삭제할 임시 여행이 없습니다.");
    }
  }
}
