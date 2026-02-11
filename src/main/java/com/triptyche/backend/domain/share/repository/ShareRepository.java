package com.triptyche.backend.domain.share.repository;

import com.triptyche.backend.domain.share.model.Share;
import com.triptyche.backend.domain.trip.model.Trip;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShareRepository extends JpaRepository<Share, Long> {

  boolean existsByTripAndRecipientId(Trip trip, Long recipientId);

  List<Share> findAllByRecipientId(Long recipientId);

  void deleteAllByTrip(Trip trip);

  Optional<Share> findByTripAndRecipientId(Trip trip, Long recipientId);

  List<Share> findAllByTrip(Trip trip);
}
