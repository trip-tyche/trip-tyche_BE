package com.triptyche.backend.domain.media.event;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImageProcessedEventListener {

  private final SimpMessagingTemplate messagingTemplate;

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onImageProcessed(ImageProcessedEvent event) {
    try {
      Map<String, Object> payload = Map.of(
          "mediaFileId", event.mediaFileId(),
          "finalUrl",    event.finalUrl(),
          "status",      "PROCESSED",
          "processedAt", event.processedAt().toString()
      );
      messagingTemplate.convertAndSend("/topic/media-processed/" + event.userId(), payload);
      log.info("[MEDIA_PROCESSED] mediaFileId={} userId={}", event.mediaFileId(), event.userId());
    } catch (Exception e) {
      log.error("[MEDIA_PROCESSED] 전송 실패: mediaFileId={} userId={}", event.mediaFileId(), event.userId(), e);
    }
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onImageProcessingFailed(ImageProcessingFailedEvent event) {
    try {
      Map<String, Object> payload = Map.of(
          "mediaFileId", event.mediaFileId(),
          "status",      "FAILED",
          "reason",      event.reason()
      );
      messagingTemplate.convertAndSend("/topic/media-processed/" + event.userId(), payload);
      log.info("[MEDIA_PROCESSING_FAILED] mediaFileId={} reason={}", event.mediaFileId(), event.reason());
    } catch (Exception e) {
      log.error("[MEDIA_PROCESSING_FAILED] 전송 실패: mediaFileId={} userId={}", event.mediaFileId(), event.userId(), e);
    }
  }
}
