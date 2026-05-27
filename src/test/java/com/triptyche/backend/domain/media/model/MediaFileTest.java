package com.triptyche.backend.domain.media.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.triptyche.backend.global.common.ResultCode;
import com.triptyche.backend.global.exception.CustomException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MediaFileTest {

  private MediaFile mediaFile;

  @BeforeEach
  void setUp() {
    mediaFile = MediaFile.builder()
        .mediaKey("test-key")
        .build();
  }

  // ── markUploaded ──────────────────────────────────────────────────────────

  @Test
  @DisplayName("markUploaded: 최초 호출 시 processingStatus가 UPLOADED로 설정된다")
  void markUploaded_success() {
    mediaFile.markUploaded();

    assertThat(mediaFile.getProcessingStatus()).isEqualTo(ProcessingStatus.UPLOADED);
  }

  @Test
  @DisplayName("markUploaded: 두 번째 호출 시 INVALID_MEDIA_STATE 예외가 발생한다")
  void markUploaded_alreadySet_throwsException() {
    mediaFile.markUploaded();

    assertThatThrownBy(() -> mediaFile.markUploaded())
        .isInstanceOf(CustomException.class)
        .satisfies(ex -> assertThat(((CustomException) ex).getResultCode())
            .isEqualTo(ResultCode.INVALID_MEDIA_STATE));
  }

  // ── markProcessing ────────────────────────────────────────────────────────

  @Test
  @DisplayName("markProcessing: UPLOADED → PROCESSING 전이 성공")
  void markProcessing_fromUploaded_success() {
    mediaFile.markUploaded();
    mediaFile.markProcessing();

    assertThat(mediaFile.getProcessingStatus()).isEqualTo(ProcessingStatus.PROCESSING);
  }

  @Test
  @DisplayName("markProcessing: null 상태에서 호출 시 INVALID_MEDIA_STATE 예외가 발생한다")
  void markProcessing_fromNull_throwsException() {
    assertThatThrownBy(() -> mediaFile.markProcessing())
        .isInstanceOf(CustomException.class)
        .satisfies(ex -> assertThat(((CustomException) ex).getResultCode())
            .isEqualTo(ResultCode.INVALID_MEDIA_STATE));
  }

  // ── markProcessed ─────────────────────────────────────────────────────────

  @Test
  @DisplayName("markProcessed: UPLOADED 상태에서 직접 성공 (processedAt 세팅)")
  void markProcessed_fromUploaded_success() {
    LocalDateTime now = LocalDateTime.now();
    mediaFile.markUploaded();
    mediaFile.markProcessed(now);

    assertThat(mediaFile.getProcessingStatus()).isEqualTo(ProcessingStatus.PROCESSED);
    assertThat(mediaFile.getProcessedAt()).isEqualTo(now);
  }

  @Test
  @DisplayName("markProcessed: PROCESSING 상태에서도 성공한다")
  void markProcessed_fromProcessing_success() {
    LocalDateTime now = LocalDateTime.now();
    mediaFile.markUploaded();
    mediaFile.markProcessing();
    mediaFile.markProcessed(now);

    assertThat(mediaFile.getProcessingStatus()).isEqualTo(ProcessingStatus.PROCESSED);
    assertThat(mediaFile.getProcessedAt()).isEqualTo(now);
  }

  @Test
  @DisplayName("markProcessed: 이미 PROCESSED 상태에서 재호출 시 INVALID_MEDIA_STATE 예외가 발생한다")
  void markProcessed_alreadyProcessed_throwsException() {
    LocalDateTime now = LocalDateTime.now();
    mediaFile.markUploaded();
    mediaFile.markProcessed(now);

    assertThatThrownBy(() -> mediaFile.markProcessed(now))
        .isInstanceOf(CustomException.class)
        .satisfies(ex -> assertThat(((CustomException) ex).getResultCode())
            .isEqualTo(ResultCode.INVALID_MEDIA_STATE));
  }

  @Test
  @DisplayName("markProcessed: 이전 failureReason을 null로 리셋한다")
  void markProcessed_resetsFailureReason() {
    // Builder로 UPLOADED 상태 + failureReason이 미리 세팅된 파일을 만든다
    MediaFile mf = MediaFile.builder()
        .mediaKey("test-key-2")
        .processingStatus(ProcessingStatus.UPLOADED)
        .failureReason("previous error")
        .build();
    assertThat(mf.getFailureReason()).isEqualTo("previous error");

    mf.markProcessed(LocalDateTime.now());

    assertThat(mf.getFailureReason()).isNull();
    assertThat(mf.getProcessingStatus()).isEqualTo(ProcessingStatus.PROCESSED);
  }

  // ── markFailed ────────────────────────────────────────────────────────────

  @Test
  @DisplayName("markFailed: null 상태에서 호출 가능하고 reason이 저장된다")
  void markFailed_fromNull_success() {
    mediaFile.markFailed("timeout");

    assertThat(mediaFile.getProcessingStatus()).isEqualTo(ProcessingStatus.FAILED);
    assertThat(mediaFile.getFailureReason()).isEqualTo("timeout");
  }

  @Test
  @DisplayName("markFailed: UPLOADED 상태에서 호출 가능하다")
  void markFailed_fromUploaded_success() {
    mediaFile.markUploaded();
    mediaFile.markFailed("parse error");

    assertThat(mediaFile.getProcessingStatus()).isEqualTo(ProcessingStatus.FAILED);
    assertThat(mediaFile.getFailureReason()).isEqualTo("parse error");
  }

  @Test
  @DisplayName("markFailed: PROCESSING 상태에서 호출 가능하다")
  void markFailed_fromProcessing_success() {
    mediaFile.markUploaded();
    mediaFile.markProcessing();
    mediaFile.markFailed("crash");

    assertThat(mediaFile.getProcessingStatus()).isEqualTo(ProcessingStatus.FAILED);
    assertThat(mediaFile.getFailureReason()).isEqualTo("crash");
  }

  @Test
  @DisplayName("markFailed: PROCESSED 상태에서 호출 시 INVALID_MEDIA_STATE 예외가 발생한다")
  void markFailed_fromProcessed_throwsException() {
    mediaFile.markUploaded();
    mediaFile.markProcessed(LocalDateTime.now());

    assertThatThrownBy(() -> mediaFile.markFailed("too late"))
        .isInstanceOf(CustomException.class)
        .satisfies(ex -> assertThat(((CustomException) ex).getResultCode())
            .isEqualTo(ResultCode.INVALID_MEDIA_STATE));
  }
}
