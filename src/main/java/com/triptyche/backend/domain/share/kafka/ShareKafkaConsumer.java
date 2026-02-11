/*
package com.triptyche.backend.domain.share.kafka;

import com.triptyche.backend.domain.share.kafka.dto.ShareCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShareKafkaConsumer {

  private final RedisTemplate<String, Object> redisTemplate;

  @KafkaListener(
          topics = "share-create",
          groupId = "share-consumer-group",
          containerFactory = "shareCreatedEventListenerContainerFactory"
  )
  private void consumerShareCreatedEvent(ShareCreatedEvent event) {
    log.info("전달받은 이벤트: {}", event);

    // 1) action 체크
    if ("CREATE".equalsIgnoreCase(event.action())) {
      // 2) Redis에 저장할 key/field 결정
      String key = "share:" + event.recipientId();
      String field = event.shareId().toString();

      // 3) 전체 이벤트 객체 그대로 넣기 (직렬화 → JSON)
      //    또는 필드만 골라서 Map 형태로 넣어도 됨
      redisTemplate.opsForHash().put(key, field, event);

      log.info("Redis에 저장 완료 key={}, field={}, event={}", key, field, event);
    } else {
      log.warn("알 수 없는 이벤트: {}", event.action());
    }
  }
}
*/
