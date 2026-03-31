package com.triptyche.backend.domain.notification.event;

import com.triptyche.backend.domain.notification.model.NotificationType;
import com.triptyche.backend.domain.share.event.ShareApprovedEvent;
import com.triptyche.backend.domain.share.event.ShareCreatedEvent;
import com.triptyche.backend.domain.share.event.ShareRejectedEvent;
import java.util.Map;
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
public class ShareNotificationEventListener {

  private final NotificationSender notificationSender;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void handleShareCreated(ShareCreatedEvent event) {
    Map<String, Object> payload = Map.of(
            "referenceId", event.shareId(),
            "type", NotificationType.SHARED_REQUEST.name(),
            "senderNickname", event.senderNickname() != null ? event.senderNickname() : "",
            "tripTitle", event.tripTitle() != null ? event.tripTitle() : ""
    );
    notificationSender.sendNotification(
            event.recipientId(),
            NotificationType.SHARED_REQUEST,
            payload,
            event.shareId(),
            event.senderNickname()
    );
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void handleShareApproved(ShareApprovedEvent event) {
    Map<String, Object> payload = Map.of(
            "recipientId", event.ownerId(),
            "type", NotificationType.SHARED_APPROVE.name()
    );
    notificationSender.sendNotification(
            event.ownerId(),
            NotificationType.SHARED_APPROVE,
            payload,
            event.shareId(),
            event.senderNickname()
    );
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void handleShareRejected(ShareRejectedEvent event) {
    Map<String, Object> payload = Map.of(
            "recipientId", event.ownerId(),
            "type", NotificationType.SHARED_REJECTED.name()
    );
    notificationSender.sendNotification(
            event.ownerId(),
            NotificationType.SHARED_REJECTED,
            payload,
            event.shareId(),
            event.senderNickname()
    );
  }
}