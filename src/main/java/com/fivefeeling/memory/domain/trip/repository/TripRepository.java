package com.fivefeeling.memory.domain.trip.repository;

import com.fivefeeling.memory.domain.trip.model.Trip;
import com.fivefeeling.memory.domain.user.model.User;
import java.time.LocalDateTime;
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

  // DRAFT 상태이며, 생성일이 임계치 이전인 Trip 조회 (임시 데이터 정리용)
  List<Trip> findByStatusAndCreatedAtBefore(String status, LocalDateTime threshold);

  // 여행갯수
  long countByUser(User user);

  // 최근여행 조회
  Optional<Trip> findFirstByUserAndStatusOrderByCreatedAtDesc(User user, String status);
}
