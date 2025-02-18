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
public class ShareApprovedEventListener {

  private final RedisTemplate<String, Object> redisTemplate;
  private final NotificationRepository notificationRepository;

  @EventListener
  public void handleShareApprovedEvent(ShareApprovedEvent event) {
    try {
      log.info("처리 중인 이벤트: {}", event);

      Map<String, Object> eventMap = Map.of(
              "recipientId", String.valueOf(event.getOwnerId()),
              "messageType", NotificationType.SHARED_APPROVE.name()
      );

      RecordId recordId = redisTemplate.opsForStream().add("shareRequests", eventMap);
      log.info("Redis Stream에 이벤트 저장 완료: {}", recordId.getValue());

      // 알림 메시지 DB 저장
      Notification notification = Notification.builder()
              .userId(event.getOwnerId())
              .message(NotificationType.SHARED_APPROVE)
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
