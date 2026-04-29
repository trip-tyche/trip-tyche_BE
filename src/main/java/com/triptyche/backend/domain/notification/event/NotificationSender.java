package com.triptyche.backend.domain.notification.event;

import com.triptyche.backend.domain.notification.model.Notification;
import com.triptyche.backend.domain.notification.model.NotificationStatus;
import com.triptyche.backend.domain.notification.model.NotificationType;
import com.triptyche.backend.domain.notification.repository.NotificationRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationSender {

  private final NotificationRepository notificationRepository;
  private final ApplicationEventPublisher eventPublisher;

  public void sendNotification(Long recipientId, NotificationType type,
                               Map<String, Object> payload, Long referenceId,
                               String senderNickname) {
    saveNotification(recipientId, type, referenceId, senderNickname);
    eventPublisher.publishEvent(new NotificationSavedEvent(recipientId, type, payload));
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
}