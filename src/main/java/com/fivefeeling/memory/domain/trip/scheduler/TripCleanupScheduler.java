package com.fivefeeling.memory.domain.trip.scheduler;

import com.fivefeeling.memory.domain.trip.model.Trip;
import com.fivefeeling.memory.domain.trip.repository.TripRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class TripCleanupScheduler {

  private final TripRepository tripRepository;

  @Scheduled(cron = "0 0 * * * *")
  @Transactional
  public void cleanupDraftTrips() {
    LocalDateTime threshold = LocalDateTime.now().minusHours(1);
    List<Trip> stableDraftTrips = tripRepository.findByStatusAndCreatedAtBefore("DRAFT", threshold);
    if (!stableDraftTrips.isEmpty()) {
      tripRepository.deleteAll(stableDraftTrips);
    }
  }
}
