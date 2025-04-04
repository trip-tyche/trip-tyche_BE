package com.fivefeeling.memory.domain.trip.repository;

import com.fivefeeling.memory.domain.trip.model.Trip;
import com.fivefeeling.memory.domain.user.model.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {

  Optional<Trip> findByTripId(Long tripId);

  @Query("""
          SELECT DISTINCT t
          FROM Trip t
              LEFT JOIN t.sharedUsers su
          WHERE (t.user.userId = :userId OR su.userId = :userId)
                    AND t.status = 'CONFIRMED'
          """)
  List<Trip> findAllAccessibleTrips(@Param("userId") Long userId);


  // 여행갯수
  long countByUser(User user);

  // 최근여행 조회
  Optional<Trip> findFirstByUserAndStatusOrderByCreatedAtDesc(User user, String status);

  List<Trip> findByStatus(String status);
}
