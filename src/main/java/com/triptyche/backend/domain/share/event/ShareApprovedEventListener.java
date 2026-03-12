package com.triptyche.backend.domain.share.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.triptyche.backend.domain.notification.model.Notification;
import com.triptyche.backend.domain.notification.model.NotificationStatus;
import com.triptyche.backend.domain.notification.model.NotificationType;
import com.triptyche.backend.domain.notification.repository.NotificationRepository;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShareApprovedEventListener {

  private final NotificationRepository notificationRepository;
  private final SimpMessagingTemplate messagingTemplate;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void handleShareApprovedEvent(ShareApprovedEvent event) {
    log.info("처리 중인 ShareCreatedEvent: {}", event);

    // 알림 메시지 DB 저장
    Notification notification = Notification.builder()
            .userId(event.getOwnerId())
            .message(NotificationType.SHARED_APPROVE)
            .status(NotificationStatus.UNREAD)
            .referenceId(event.getShareId())
            .senderNickname(event.getSenderNickname())
            .build();
    notificationRepository.save(notification);
    log.info("💾 DB 저장 완료 (SHARED_REQUEST): notificationId={}", notification.getNotificationId());

    try {
      // 2) WebSocket으로 JSON 페이로드 전송
      Map<String, Object> payload = new HashMap<>();
      payload.put("recipientId", event.getOwnerId());
      payload.put("type", NotificationType.SHARED_APPROVE.name());

      String jsonPayload = objectMapper.writeValueAsString(payload);
      messagingTemplate.convertAndSend(
              "/topic/share-notifications/" + event.getOwnerId(),
              jsonPayload
      );
      log.info("📤[SHARED_APPROVE] WebSocket 전송 완료 → {}", jsonPayload);
    } catch (Exception e) {
      log.error("❌ WebSocket 전송 실패 (SHARED_APPROVE)", e);
    }
  }
}
