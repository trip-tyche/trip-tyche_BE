package com.triptyche.backend.domain.share.repository;

import com.triptyche.backend.domain.share.dto.ShareSummary;
import com.triptyche.backend.domain.share.model.Share;
import com.triptyche.backend.domain.share.model.ShareStatus;
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

  void deleteAllByTrip(Trip trip);

  List<Share> findAllByTrip(Trip trip);

  boolean existsByTripAndRecipientIdAndShareStatus(
      Trip trip, Long recipientId, ShareStatus shareStatus);

  List<Share> findAllByTripTripId(Long tripId);

  void deleteAllByTripIn(List<Trip> trips);

  @Query("""
      SELECT s FROM Share s
      JOIN FETCH s.trip t
      JOIN FETCH t.user
      WHERE s.shareId = :shareId
      """)
  Optional<Share> findByIdWithTripAndOwner(@Param("shareId") Long shareId);

  @Query("""
          SELECT new com.triptyche.backend.domain.share.dto.ShareSummary(
              s.trip.tripId, s.recipientId, s.shareId, u.userNickName
          )
          FROM Share s
          JOIN User u ON u.userId = s.recipientId
          WHERE s.trip.tripId IN :tripIds
            AND s.shareStatus = 'APPROVED'
          """)
  List<ShareSummary> findApprovedShareSummariesByTripIds(@Param("tripIds") List<Long> tripIds);
}
