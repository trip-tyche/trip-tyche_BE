/*
package com.fivefeeling.memory.domain.share.kafka;

import com.fivefeeling.memory.domain.share.kafka.dto.ShareCreatedEvent;
import com.fivefeeling.memory.domain.share.model.Share;
import com.fivefeeling.memory.domain.trip.model.Trip;
import com.fivefeeling.memory.domain.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ShareKafkaProducer {

  private final KafkaTemplate<String, Object> kafkaTemplate;

  private static final String SHARE_CREATE_TOPIC = "share-create";

  public void sendShareCreatedEvent(Share share) {
    Trip trip = share.getTrip();
    String tripTitle = trip.getTripTitle();              // TripTitle
    User owner = trip.getUser();                         // Trip 주인
    String ownerNickName = owner.getUserNickName();      // 닉네임
    String shareStatus = share.getShareStatus().name();  // Enum → 문자열
    Long recipientId = share.getRecipientId();

    ShareCreatedEvent event = new ShareCreatedEvent(
            share.getShareId(),
            trip.getTripId(),
            tripTitle,
            ownerNickName,
            recipientId,
            shareStatus,
            "CREATE"  // action
    );

    kafkaTemplate.send(SHARE_CREATE_TOPIC, event);
  }
}
*/
