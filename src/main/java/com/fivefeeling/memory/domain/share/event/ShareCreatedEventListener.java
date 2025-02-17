package com.fivefeeling.memory.domain.share.event;

import com.fivefeeling.memory.domain.notification.model.Notification;
import com.fivefeeling.memory.domain.notification.model.NotificationStatus;
import com.fivefeeling.memory.domain.notification.model.NotificationType;
import com.fivefeeling.memory.domain.notification.repository.NotificationRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShareCreatedEventListener {


  private final RedisTemplate<String, Object> redisTemplate;
  private final NotificationRepository notificationRepository;

  @EventListener
  public void handleShareCreatedEvent(ShareCreatedEvent event) {
    try {
      log.info("처리 중인 이벤트: {}", event);

      Map<String, Object> eventMap = Map.of(
              "recipientId", String.valueOf(event.getRecipientId()),
              "messageType", String.valueOf(NotificationType.SHARED_REQUEST)
      );
      RecordId recordId = redisTemplate.opsForStream().add("shareRequests", eventMap);
      log.info("Redis Stream에 이벤트 저장 및 발행 완료: {}", event);

      // 알림 메시지 DB 저장
      Notification notification = Notification.builder()
              .userId(event.getRecipientId())
              .message(NotificationType.SHARED_REQUEST)
              .status(NotificationStatus.UNREAD)
              .streamMessageId(recordId.getValue())
              .referenceId(event.getShareId())
              .senderNickname(event.getSenderNickname())
              .build();
      notificationRepository.save(notification);
      log.info("DB에 알림 메시지 저장 완료: {}", notification);

    } catch (Exception e) {
      log.error("이벤트 처리 중 오류 발생", e);
    }
  }
}
