package com.fivefeeling.memory.domain.trip.repository;

import com.fivefeeling.memory.domain.trip.model.Trip;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {

  List<Trip> findByUserUserId(Long userId);

  Optional<Trip> findByTripId(Long tripId);
}
