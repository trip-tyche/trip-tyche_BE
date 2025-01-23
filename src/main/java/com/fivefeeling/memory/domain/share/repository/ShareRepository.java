package com.fivefeeling.memory.domain.share.repository;

import com.fivefeeling.memory.domain.share.model.Share;
import com.fivefeeling.memory.domain.trip.model.Trip;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShareRepository extends JpaRepository<Share, Long> {

  boolean existsByTripAndRecipientId(Trip trip, Long recipientId);

  Optional<Share> findByRecipientId(Long recipientId);

  List<Share> findAllByRecipientId(Long recipientId);
}
