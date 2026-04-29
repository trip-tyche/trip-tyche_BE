package com.triptyche.backend.domain.notification.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationWebSocketListener {

  private final SimpMessagingTemplate messagingTemplate;

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onNotificationSaved(NotificationSavedEvent event) {
    try {
      messagingTemplate.convertAndSend(
              "/topic/share-notifications/" + event.recipientId(),
              event.payload()
      );
      log.info("[{}] 알림 전송 완료: recipient={}", event.type(), event.recipientId());
    } catch (Exception e) {
      log.error("[{}] 알림 전송 실패: recipient={}", event.type(), event.recipientId(), e);
    }
  }
}