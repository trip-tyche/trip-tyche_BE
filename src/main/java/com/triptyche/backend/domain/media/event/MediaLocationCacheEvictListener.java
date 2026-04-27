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
public class MediaLocationCacheEvictListener {

  private final UnlocatedMediaCacheService unlocatedMediaCacheService;

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleCacheEvict(MediaLocationCacheEvictRequestedEvent event) {
    unlocatedMediaCacheService.evict(event.tripId(), event.mediaFileId());
  }
}