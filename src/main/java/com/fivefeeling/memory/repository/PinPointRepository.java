package com.fivefeeling.memory.repository;

import com.fivefeeling.memory.entity.PinPoint;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PinPointRepository extends JpaRepository<PinPoint, Long> {

  List<PinPoint> findByTripTripId(Long tripId);

}
