package com.triptyche.backend.domain.share.repository;

import com.triptyche.backend.domain.share.dto.ShareSummaryResponse;
import com.triptyche.backend.domain.share.model.Share;
import com.triptyche.backend.domain.trip.model.Trip;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ShareRepository extends JpaRepository<Share, Long> {

  boolean existsByTripAndRecipientId(Trip trip, Long recipientId);

  List<Share> findAllByRecipientId(Long recipientId);

  void deleteAllByTripIn(List<Trip> trips);

  @Query("""
      SELECT s FROM Share s
      JOIN FETCH s.trip t
      JOIN FETCH t.user
      WHERE s.shareId = :shareId
      """)
  Optional<Share> findByIdWithTripAndOwner(@Param("shareId") Long shareId);

  @Query("""
      SELECT s.recipientId FROM Share s
      WHERE s.trip.tripId = :tripId
        AND s.shareStatus = 'APPROVED'
      """)
  List<Long> findApprovedRecipientIdsByTripId(@Param("tripId") Long tripId);

  @Query("""
      SELECT s.recipientId FROM Share s
      WHERE s.trip = :trip
        AND s.shareStatus = 'APPROVED'
      """)
  List<Long> findApprovedRecipientIdsByTrip(@Param("trip") Trip trip);

  @Query("""
          SELECT new com.triptyche.backend.domain.share.dto.ShareSummaryResponse(
              s.trip.tripId, s.recipientId, s.shareId, u.userNickName
          )
          FROM Share s
          JOIN User u ON u.userId = s.recipientId
          WHERE s.trip.tripId IN :tripIds
            AND s.shareStatus = 'APPROVED'
          """)
  List<ShareSummaryResponse> findApprovedShareSummariesByTripIds(@Param("tripIds") List<Long> tripIds);
}
