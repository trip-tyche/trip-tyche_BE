package com.fivefeeling.memory.global.websocket;

import com.fivefeeling.memory.global.redis.RedisStreamMessageListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketNotificationHandler {

  private final RedisStreamMessageListener redisStreamMessageListener;
  private final SimpMessagingTemplate messagingTemplate;

  @MessageMapping("/notification-count")
  public void getNotificationCount(NotificationRequest request) {
    log.info("알림 갯수 요청: recipientId={}", request.recipientId());

    int unreadCount = redisStreamMessageListener.getUnreadNotificationCount(request.recipientId());
    log.info("알림 갯수 응답: recipientId={}, unreadCount={}", request.recipientId(), unreadCount);

    messagingTemplate.convertAndSend(
            "/topic/share-notifications/" + request.recipientId(),
            new NotificationCountResponse(request.recipientId(), unreadCount)
    );
    log.info("✅ WebSocket 메시지 전송 완료: /topic/share-notifications/{}, unreadCount={}", request.recipientId(),
            unreadCount);
  }
}
