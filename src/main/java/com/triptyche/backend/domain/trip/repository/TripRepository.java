package com.triptyche.backend.domain.trip.repository;

import com.triptyche.backend.domain.trip.model.Trip;
import com.triptyche.backend.domain.trip.model.TripStatus;
import com.triptyche.backend.domain.user.model.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {

  @Query("SELECT t.tripTitle FROM Trip t WHERE t.tripId = :tripId")
  Optional<String> findTripTitleById(@Param("tripId") Long tripId);

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


  long countByUserAndStatus(User user, TripStatus status);


  List<Trip> findByStatus(TripStatus status);
}
