package com.fivefeeling.memory.repository;

import com.fivefeeling.memory.entity.Trip;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {

  List<Trip> findByUserUserId(Long userId);
}
