package com.fivefeeling.memory.domain.share.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fivefeeling.memory.domain.notification.model.Notification;
import com.fivefeeling.memory.domain.notification.model.NotificationStatus;
import com.fivefeeling.memory.domain.notification.model.NotificationType;
import com.fivefeeling.memory.domain.notification.repository.NotificationRepository;
import com.fivefeeling.memory.domain.trip.repository.TripRepository;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShareCreatedEventListener {

  private final NotificationRepository notificationRepository;
  private final SimpMessagingTemplate messagingTemplate;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final TripRepository tripRepository;


  @EventListener
  @Transactional
  public void handleShareCreatedEvent(ShareCreatedEvent event) {
    log.info("처리 중인 ShareCreatedEvent: {}", event);
    Long tripId = event.getTripId();

    // 1) DB에 알림 저장
    Notification notification = Notification.builder()
            .userId(event.getRecipientId())
            .message(NotificationType.SHARED_REQUEST)
            .status(NotificationStatus.UNREAD)
            .referenceId(event.getShareId())
            .senderNickname(event.getSenderNickname())
            .build();
    notificationRepository.save(notification);
    log.info("💾DB 저장 완료 (SHARED_REQUEST): notificationId={}", notification.getNotificationId());

    try {
      // 2) WebSocket으로 JSON 페이로드 전송
      Map<String, Object> payload = new HashMap<>();
      payload.put("referenceId", event.getShareId());
      payload.put("type", NotificationType.SHARED_REQUEST.name());
      payload.put("tripTitle", tripRepository.findTripTitleById(tripId).orElse("UNKNOWN_TRIP"));
      payload.put("senderNickname", event.getSenderNickname());

      String jsonPayload = objectMapper.writeValueAsString(payload);
      messagingTemplate.convertAndSend(
              "/topic/share-notifications/" + event.getRecipientId(),
              jsonPayload
      );
      log.info("📤[SHARED_REQUEST] WebSocket 전송 완료 → {}", jsonPayload);
    } catch (Exception e) {
      log.error("❌ WebSocket 전송 실패 (SHARED_REQUEST)", e);
    }
  }
}
