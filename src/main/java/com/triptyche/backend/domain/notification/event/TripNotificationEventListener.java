package com.triptyche.backend.domain.notification.event;

import com.triptyche.backend.domain.notification.model.NotificationType;
import com.triptyche.backend.domain.share.service.ShareQueryService;
import com.triptyche.backend.domain.trip.event.TripDeletedEvent;
import com.triptyche.backend.domain.trip.event.TripUpdatedEvent;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class TripNotificationEventListener {

  private final NotificationSender notificationSender;
  private final ShareQueryService shareQueryService;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void onTripUpdated(TripUpdatedEvent event) {
    processTripUpdatedEvent(event, NotificationType.TRIP_UPDATED);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void onTripDeleted(TripDeletedEvent event) {
    processTripDeletedEvent(
            event.tripId(),
            event.tripTitle(),
            event.ownerNickname(),
            NotificationType.TRIP_DELETED
    );
  }

  /**
   * Trip 수정 이벤트 처리 (소유자/공유자 모두 가능)
   */
  private void processTripUpdatedEvent(TripUpdatedEvent event, NotificationType type) {
    Set<Long> recipientIds = determineTripUpdateRecipients(
            event.tripId(), event.ownerId(), event.actorId(), event.isOwner());

    recipientIds.forEach(recipientId ->
            notificationSender.sendNotification(
                    recipientId,
                    type,
                    buildTripUpdatePayload(recipientId, event.tripKey(),
                            event.tripTitle(), event.actorNickname(), type),
                    event.tripId(),
                    event.actorNickname()
            )
    );
  }

  /**
   * Trip 삭제 이벤트 처리 (소유자만 가능)
   */
  private void processTripDeletedEvent(
      Long tripId,
      String tripTitle,
      String ownerNickname,
      NotificationType type) {

    Set<Long> recipientIds = getApprovedShareRecipientsByTripId(tripId);
    recipientIds.forEach(recipientId ->
            notificationSender.sendNotification(
                    recipientId,
                    type,
                    buildDeletedTripPayload(recipientId, tripTitle, ownerNickname, type),
                    tripId,
                    ownerNickname
            )
    );
  }

  private Set<Long> determineTripUpdateRecipients(Long tripId, Long ownerId,
                                                  Long actorId, boolean isOwner) {
    Set<Long> recipientIds = new HashSet<>();
    Set<Long> approvedShareIds = getApprovedShareRecipientsByTripId(tripId);

    if (isOwner) {
      recipientIds.addAll(approvedShareIds);
    } else {
      recipientIds.add(ownerId);
      approvedShareIds.stream()
              .filter(id -> !id.equals(actorId))
              .forEach(recipientIds::add);
    }

    return recipientIds;
  }

  private Set<Long> getApprovedShareRecipientsByTripId(Long tripId) {
    return shareQueryService.findApprovedRecipientIdsByTripId(tripId);
  }

  /**
   * Trip 수정 이벤트용 페이로드 생성
   */
  private Map<String, Object> buildTripUpdatePayload(Long recipientId, String tripKey,
                                                     String tripTitle, String senderNickname,
                                                     NotificationType type) {
    return Map.of(
            "recipientId", recipientId,
            "type", type.name(),
            "tripKey", tripKey,
            "tripTitle", tripTitle != null ? tripTitle : "",
            "senderNickname", senderNickname
    );
  }

  private Map<String, Object> buildDeletedTripPayload(
      Long recipientId,
      String tripTitle,
      String ownerNickname,
      NotificationType type) {

    return Map.of(
            "recipientId", recipientId,
            "type", type.name(),
            "tripTitle", tripTitle != null ? tripTitle : "",
            "senderNickname", ownerNickname
    );
  }
}