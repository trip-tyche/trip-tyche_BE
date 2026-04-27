package com.triptyche.backend.global.redis;

import com.triptyche.backend.domain.media.event.MediaFileRegisteredEvent;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImageProcessingPublisher {

  private static final String STREAM_KEY = "image-processing-stream";

  private final RedisTemplate<String, Object> redisTemplate;

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void publish(MediaFileRegisteredEvent event) {
    try {
      redisTemplate.opsForStream().add(
              MapRecord.create(STREAM_KEY, Map.of(
                      "mediaFileId", String.valueOf(event.mediaFileId()),
                      "originalKey", event.originalKey()
              ))
      );
    } catch (Exception e) {
      log.error("Redis Stream XADD 실패 - mediaFileId: {}", event.mediaFileId(), e);
    }
  }
}