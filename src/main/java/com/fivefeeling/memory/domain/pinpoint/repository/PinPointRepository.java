package com.fivefeeling.memory.domain.pinpoint.repository;

import com.fivefeeling.memory.domain.pinpoint.model.PinPoint;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PinPointRepository extends JpaRepository<PinPoint, Long> {

  @EntityGraph(attributePaths = {"mediaFiles"})
  List<PinPoint> findByTripTripId(Long tripId);

}
