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

  // 접근 권한이 있는 여행(소유하거나 공유된 사용자, 상태가 CONFIRMED)만 반환하는 메서드
  @Query("""
          SELECT DISTINCT t
          FROM Trip t
              LEFT JOIN t.sharedUsers su
          WHERE (t.user.userId = :userId OR su.userId = :userId)
            AND t.tripId = :tripId
          """)
  Optional<Trip> findAccessibleTrip(@Param("tripId") Long tripId,
                                    @Param("userId") Long userId);

  Optional<Trip> findByTripKey(String tripKey);

  @Query("""
          SELECT DISTINCT t
          FROM Trip t
              LEFT JOIN t.sharedUsers su
          WHERE (t.user.userId = :userId OR su.userId = :userId)
                    AND (t.status = 'CONFIRMED' OR t.status = 'IMAGES_UPLOADED')
          """)
  List<Trip> findAllAccessibleTrips(@Param("userId") Long userId);


  long countByUserAndStatus(User user, String status);

  // 최근여행 조회
  Optional<Trip> findFirstByUserAndStatusOrderByCreatedAtDesc(User user, String status);

  List<Trip> findByStatus(String status);
}
