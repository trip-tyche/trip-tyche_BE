package com.triptyche.backend.domain.media.event;

import com.triptyche.backend.global.s3.S3UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class MediaFileS3DeleteEventListener {

  private final S3UploadService s3UploadService;

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleS3Delete(MediaFilesS3DeleteRequestedEvent event) {
    if (event.mediaKeys() == null || event.mediaKeys().isEmpty()) {
      return;
    }
    try {
      s3UploadService.deleteFiles(event.mediaKeys());
    } catch (Exception e) {
      log.error("S3 파일 삭제 실패 — keys: {}, error: {}", event.mediaKeys(), e.getMessage(), e);
    }
  }
}