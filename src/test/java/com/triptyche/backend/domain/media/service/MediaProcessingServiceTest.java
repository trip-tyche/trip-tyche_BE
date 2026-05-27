package com.triptyche.backend.domain.media.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.triptyche.backend.domain.media.event.ImageProcessedEvent;
import com.triptyche.backend.domain.media.event.ImageProcessingFailedEvent;
import com.triptyche.backend.domain.media.model.MediaFile;
import com.triptyche.backend.domain.media.model.ProcessingStatus;
import com.triptyche.backend.domain.media.repository.MediaFileRepository;
import com.triptyche.backend.domain.trip.model.Trip;
import com.triptyche.backend.domain.user.model.User;
import com.triptyche.backend.global.common.ResultCode;
import com.triptyche.backend.global.exception.CustomException;
import com.triptyche.backend.global.s3.S3KeyResolver;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class MediaProcessingServiceTest {

  @Mock
  private MediaFileRepository mediaFileRepository;

  @Mock
  private S3KeyResolver s3KeyResolver;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @InjectMocks
  private MediaProcessingService mediaProcessingService;

  private static final Long MEDIA_FILE_ID = 1L;
  private static final Long USER_ID = 10L;
  private static final String FINAL_KEY = "processed/42/abc.webp";
  private static final String FINAL_URL = "https://cdn.example.com/processed/42/abc.webp";
  private static final LocalDateTime PROCESSED_AT = LocalDateTime.of(2024, 6, 1, 12, 0);

  private MediaFile buildMediaFile(ProcessingStatus status) {
    User user = User.builder()
        .userId(USER_ID)
        .userName("테스터")
        .userNickName("tester")
        .userEmail("tester@example.com")
        .provider("google")
        .build();
    Trip trip = Trip.builder()
        .tripTitle("테스트 여행")
        .country("일본")
        .user(user)
        .build();
    MediaFile mf = MediaFile.builder()
        .trip(trip)
        .mediaKey("originals/tripKey/file.jpg")
        .mediaLink("https://cdn.example.com/originals/tripKey/file.jpg")
        .processingStatus(status)
        .build();
    return mf;
  }

  @Nested
  @DisplayName("applyProcessedResult()")
  class ApplyProcessedResult {

    @Test
    @DisplayName("PROCESSING 상태의 MediaFile을 PROCESSED로 전이하고 ImageProcessedEvent를 발행한다")
    void applyProcessedResult_givenProcessingFile_transitionsToProcessedAndPublishesEvent() {
      MediaFile mf = buildMediaFile(ProcessingStatus.PROCESSING);
      given(mediaFileRepository.findById(MEDIA_FILE_ID)).willReturn(Optional.of(mf));
      given(s3KeyResolver.buildUrl(FINAL_KEY)).willReturn(FINAL_URL);

      mediaProcessingService.applyProcessedResult(MEDIA_FILE_ID, FINAL_KEY, PROCESSED_AT);

      assertThat(mf.getProcessingStatus()).isEqualTo(ProcessingStatus.PROCESSED);
      assertThat(mf.getMediaLink()).isEqualTo(FINAL_URL);

      ArgumentCaptor<ImageProcessedEvent> captor = ArgumentCaptor.forClass(ImageProcessedEvent.class);
      verify(eventPublisher).publishEvent(captor.capture());
      ImageProcessedEvent event = captor.getValue();
      assertThat(event.mediaFileId()).isEqualTo(MEDIA_FILE_ID);
      assertThat(event.userId()).isEqualTo(USER_ID);
      assertThat(event.finalUrl()).isEqualTo(FINAL_URL);
      assertThat(event.processedAt()).isEqualTo(PROCESSED_AT);
    }

    @Test
    @DisplayName("UPLOADED 상태의 MediaFile도 PROCESSED로 전이할 수 있다")
    void applyProcessedResult_givenUploadedFile_transitionsToProcessed() {
      MediaFile mf = buildMediaFile(ProcessingStatus.UPLOADED);
      given(mediaFileRepository.findById(MEDIA_FILE_ID)).willReturn(Optional.of(mf));
      given(s3KeyResolver.buildUrl(FINAL_KEY)).willReturn(FINAL_URL);

      mediaProcessingService.applyProcessedResult(MEDIA_FILE_ID, FINAL_KEY, PROCESSED_AT);

      assertThat(mf.getProcessingStatus()).isEqualTo(ProcessingStatus.PROCESSED);
      verify(eventPublisher).publishEvent(any(ImageProcessedEvent.class));
    }

    @Test
    @DisplayName("이미 PROCESSED 상태이면 멱등하게 처리되고 이벤트는 발행되지 않는다")
    void applyProcessedResult_givenAlreadyProcessedFile_isIdempotent() {
      MediaFile mf = buildMediaFile(ProcessingStatus.PROCESSED);
      given(mediaFileRepository.findById(MEDIA_FILE_ID)).willReturn(Optional.of(mf));

      mediaProcessingService.applyProcessedResult(MEDIA_FILE_ID, FINAL_KEY, PROCESSED_AT);

      verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("존재하지 않는 mediaFileId이면 MEDIA_FILE_NOT_FOUND 예외가 발생한다")
    void applyProcessedResult_givenMissingMediaFile_throwsNotFound() {
      given(mediaFileRepository.findById(MEDIA_FILE_ID)).willReturn(Optional.empty());

      assertThatThrownBy(() -> mediaProcessingService.applyProcessedResult(MEDIA_FILE_ID, FINAL_KEY, PROCESSED_AT))
          .isInstanceOf(CustomException.class)
          .extracting(e -> ((CustomException) e).getResultCode())
          .isEqualTo(ResultCode.MEDIA_FILE_NOT_FOUND);

      verify(eventPublisher, never()).publishEvent(any());
    }
  }

  @Nested
  @DisplayName("applyFailedResult()")
  class ApplyFailedResult {

    private static final String REASON = "처리 타임아웃";

    @Test
    @DisplayName("PROCESSING 상태의 MediaFile을 FAILED로 전이하고 ImageProcessingFailedEvent를 발행한다")
    void applyFailedResult_givenProcessingFile_transitionsToFailedAndPublishesEvent() {
      MediaFile mf = buildMediaFile(ProcessingStatus.PROCESSING);
      given(mediaFileRepository.findById(MEDIA_FILE_ID)).willReturn(Optional.of(mf));

      mediaProcessingService.applyFailedResult(MEDIA_FILE_ID, REASON);

      assertThat(mf.getProcessingStatus()).isEqualTo(ProcessingStatus.FAILED);

      ArgumentCaptor<ImageProcessingFailedEvent> captor = ArgumentCaptor.forClass(ImageProcessingFailedEvent.class);
      verify(eventPublisher).publishEvent(captor.capture());
      ImageProcessingFailedEvent event = captor.getValue();
      assertThat(event.mediaFileId()).isEqualTo(MEDIA_FILE_ID);
      assertThat(event.userId()).isEqualTo(USER_ID);
      assertThat(event.reason()).isEqualTo(REASON);
    }

    @Test
    @DisplayName("이미 PROCESSED 상태이면 멱등하게 처리되고 이벤트는 발행되지 않는다")
    void applyFailedResult_givenAlreadyProcessedFile_isIdempotent() {
      MediaFile mf = buildMediaFile(ProcessingStatus.PROCESSED);
      given(mediaFileRepository.findById(MEDIA_FILE_ID)).willReturn(Optional.of(mf));

      mediaProcessingService.applyFailedResult(MEDIA_FILE_ID, REASON);

      verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("존재하지 않는 mediaFileId이면 MEDIA_FILE_NOT_FOUND 예외가 발생한다")
    void applyFailedResult_givenMissingMediaFile_throwsNotFound() {
      given(mediaFileRepository.findById(MEDIA_FILE_ID)).willReturn(Optional.empty());

      assertThatThrownBy(() -> mediaProcessingService.applyFailedResult(MEDIA_FILE_ID, REASON))
          .isInstanceOf(CustomException.class)
          .extracting(e -> ((CustomException) e).getResultCode())
          .isEqualTo(ResultCode.MEDIA_FILE_NOT_FOUND);

      verify(eventPublisher, never()).publishEvent(any());
    }
  }
}
