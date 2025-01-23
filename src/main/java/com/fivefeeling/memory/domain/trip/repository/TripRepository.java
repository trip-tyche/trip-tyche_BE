package com.fivefeeling.memory.domain.trip.repository;

import com.fivefeeling.memory.domain.trip.model.Trip;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {

  List<Trip> findByUserUserId(Long userId);

  Optional<Trip> findByTripId(Long tripId);

  @Query("SELECT t FROM Trip t JOIN t.sharedUsers s WHERE s.userId = :userId")
  List<Trip> findAllBySharedUserId(Long userId);
}
