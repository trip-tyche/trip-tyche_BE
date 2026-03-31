package com.triptyche.backend.domain.notification.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.triptyche.backend.domain.notification.model.Notification;
import com.triptyche.backend.domain.notification.model.NotificationStatus;
import com.triptyche.backend.domain.notification.model.NotificationType;
import com.triptyche.backend.domain.notification.repository.NotificationRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationSender {

  private final NotificationRepository notificationRepository;
  private final SimpMessagingTemplate messagingTemplate;
  private final ObjectMapper objectMapper;

  /**
   * 알림 저장 및 웹소켓 전송
   */
  public void sendNotification(Long recipientId, NotificationType type,
                               Map<String, Object> payload, Long referenceId,
                               String senderNickname) {
    saveNotification(recipientId, type, referenceId, senderNickname);
    sendWebSocketMessage(recipientId, type, payload);
  }

  private void saveNotification(Long recipientId, NotificationType type,
                                Long referenceId, String senderNickname) {
    Notification notification = Notification.builder()
            .userId(recipientId)
            .message(type)
            .status(NotificationStatus.UNREAD)
            .referenceId(referenceId)
            .senderNickname(senderNickname)
            .build();
    notificationRepository.save(notification);
  }

  private void sendWebSocketMessage(Long recipientId, NotificationType type,
                                    Map<String, Object> payload) {
    try {
      String json = objectMapper.writeValueAsString(payload);
      messagingTemplate.convertAndSend(
              "/topic/share-notifications/" + recipientId,
              json
      );
      log.info("[{}] 알림 전송 → recipient: {}", type, recipientId);
    } catch (Exception e) {
      log.error("[{}] 알림 전송 실패 → recipient: {}", type, recipientId, e);
    }
  }
}