package com.triptyche.backend.domain.media.event;

import com.triptyche.backend.domain.media.service.UnlocatedMediaCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class MediaFileZeroLocationCacheListener {

  private final UnlocatedMediaCacheService unlocatedMediaCacheService;

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleZeroLocationCache(MediaFileZeroLocationCacheRequestedEvent event) {
    try {
      unlocatedMediaCacheService.save(
              event.tripId(), event.mediaFileId(), event.mediaLink(), event.recordDate());
    } catch (Exception e) {
      log.error("위치없는 미디어 Redis 캐시 저장 실패 — tripId: {}, mediaFileId: {}, error: {}",
              event.tripId(), event.mediaFileId(), e.getMessage(), e);
    }
  }
}