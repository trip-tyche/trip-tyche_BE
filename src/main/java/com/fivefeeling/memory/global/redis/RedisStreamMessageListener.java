package com.fivefeeling.memory.global.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fivefeeling.memory.domain.notification.model.NotificationType;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisStreamMessageListener implements StreamListener<String, MapRecord<String, String, String>> {

  private final SimpMessagingTemplate messagingTemplate;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public void onMessage(MapRecord<String, String, String> message) {
    try {
      log.debug("Redis Stream 메시지 수신: {}", message);

      String recipientId = message.getValue().get("recipientId");
      String typeString = message.getValue().get("messageType");

      if (recipientId == null || typeString == null) {
        log.error("Redis Stream 메시지에서 필수 데이터가 누락되었습니다: {}", message.getValue());
        return;
      }

      /*// ShareCreatedEvent 객체 생성
      ShareCreatedEvent event = new ShareCreatedEvent(
              Long.parseLong(shareId),
              Long.parseLong(tripId),
              Long.parseLong(recipientId)
      );
      log.info("Redis Stream 메시지를 ShareCreatedEvent로 변환: {}", event);
      */

      // WebSocket으로 알림 메시지 전송
      Map<String, Object> notificationMessage = new HashMap<>();
      notificationMessage.put("recipientId", Long.parseLong(recipientId));
      notificationMessage.put("type", typeString);

      String jsonPayload = objectMapper.writeValueAsString(notificationMessage);

      messagingTemplate.convertAndSend(
              "/topic/share-notifications/" + recipientId,
              jsonPayload
      );

      log.info("WebSocket으로 메시지 전송 완료: {}", recipientId, NotificationType.valueOf(typeString));
    } catch (Exception e) {
      log.error("Redis Stream 메시지 처리 실패", e);
    }
  }
}
