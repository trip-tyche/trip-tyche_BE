package com.triptyche.backend.domain.media.event;

import com.triptyche.backend.global.redis.ImageQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class MediaLocationCacheEvictListener {

  private final ImageQueueService imageQueueService;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleCacheEvict(MediaLocationCacheEvictRequestedEvent event) {
    String redisKey = "trip:" + event.tripId();
    try {
      imageQueueService.deleteFromImageQueue(redisKey, String.valueOf(event.mediaFileId()));
    } catch (Exception e) {
      log.error("Redis 위치 캐시 삭제 실패 — tripId: {}, mediaFileId: {}, error: {}",
              event.tripId(), event.mediaFileId(), e.getMessage(), e);
    }
  }
}