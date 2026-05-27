package com.triptyche.backend.domain.media.service;

import com.triptyche.backend.domain.media.event.ImageProcessedEvent;
import com.triptyche.backend.domain.media.event.ImageProcessingFailedEvent;
import com.triptyche.backend.domain.media.model.MediaFile;
import com.triptyche.backend.domain.media.model.ProcessingStatus;
import com.triptyche.backend.domain.media.repository.MediaFileRepository;
import com.triptyche.backend.global.common.ResultCode;
import com.triptyche.backend.global.exception.CustomException;
import com.triptyche.backend.global.s3.S3KeyResolver;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MediaProcessingService {

  private final MediaFileRepository mediaFileRepository;
  private final S3KeyResolver s3KeyResolver;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional
  public void applyProcessedResult(Long mediaFileId, String finalKey, LocalDateTime processedAt) {
    MediaFile mf = mediaFileRepository.findById(mediaFileId)
        .orElseThrow(() -> new CustomException(ResultCode.MEDIA_FILE_NOT_FOUND));

    if (mf.getProcessingStatus() == ProcessingStatus.PROCESSED) return;

    mf.markProcessed(processedAt);

    String finalUrl = s3KeyResolver.buildUrl(finalKey);
    mf.updateMediaLink(finalUrl);

    Long userId = mf.getTrip().getUser().getUserId();
    eventPublisher.publishEvent(new ImageProcessedEvent(mediaFileId, userId, finalUrl, processedAt));
  }

  @Transactional
  public void applyFailedResult(Long mediaFileId, String reason) {
    MediaFile mf = mediaFileRepository.findById(mediaFileId)
        .orElseThrow(() -> new CustomException(ResultCode.MEDIA_FILE_NOT_FOUND));

    if (mf.getProcessingStatus() == ProcessingStatus.PROCESSED) return;

    mf.markFailed(reason);

    Long userId = mf.getTrip().getUser().getUserId();
    eventPublisher.publishEvent(new ImageProcessingFailedEvent(mediaFileId, userId, reason));
  }
}
