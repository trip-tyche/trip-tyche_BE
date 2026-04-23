package com.triptyche.backend.domain.media.event;

import com.triptyche.backend.domain.media.service.UnlocatedMediaCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class MediaLocationCacheEvictListener {

  private final UnlocatedMediaCacheService unlocatedMediaCacheService;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleCacheEvict(MediaLocationCacheEvictRequestedEvent event) {
    unlocatedMediaCacheService.evict(event.tripId(), event.mediaFileId());
  }
}