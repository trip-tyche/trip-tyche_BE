package com.triptyche.backend.domain.media.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MediaOrphanCleanupScheduler {

  private final MediaOrphanCleanupExecutor executor;

  @Scheduled(cron = "${triptyche.media.orphan-cleanup.cron:0 0 */6 * * *}")
  public void run() {
    try {
      int deleted = executor.cleanup();
      if (deleted > 0) {
        log.info("[MEDIA_ORPHAN_CLEANUP] 삭제 완료 — count={}", deleted);
      }
    } catch (Exception e) {
      log.error("[MEDIA_ORPHAN_CLEANUP] 실패", e);
    }
  }
}
