package com.fivefeeling.memory.global.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fivefeeling.memory.domain.notification.model.NotificationType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisStreamMessageListener implements StreamListener<String, MapRecord<String, String, String>> {

  private final SimpMessagingTemplate messagingTemplate;
  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public int getUnreadNotificationCount(String recipientId) {
    String streamKey = "shareRequests";

    try {
      List<MapRecord<String, Object, Object>> records = redisTemplate
              .opsForStream()
              .range(streamKey, Range.unbounded());

      if (records == null) {
        return 0;
      }

      long count = records.stream()
              .filter(record -> {
                Object value = record.getValue().get("recipientId");
                return value != null && recipientId.equals(String.valueOf(value));
              })
              .count();
      return (int) count;
    } catch (Exception e) {
      log.error("Redis Streams에서 알림 개수 조회 실패: recipientId={}", recipientId, e);
      return 0;
    }
  }

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

      // 메시지 타입에 따라 다른 필드를 추가합니다.
      if ("NOTIFICATION_COUNT".equals(typeString)) {
        // unreadCount를 조회하는 메서드를 호출하여 값을 넣습니다.
        int unreadCount = getUnreadNotificationCount(recipientId);
        notificationMessage.put("unreadCount", unreadCount);
      } else {
        // 다른 타입의 경우 type 필드를 그대로 사용
        notificationMessage.put("type", typeString);
      }

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
