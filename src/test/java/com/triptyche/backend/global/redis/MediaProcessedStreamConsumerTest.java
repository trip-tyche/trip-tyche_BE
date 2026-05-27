package com.triptyche.backend.global.redis;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.triptyche.backend.domain.media.service.MediaProcessingService;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.MapRecord;

@ExtendWith(MockitoExtension.class)
class MediaProcessedStreamConsumerTest {

  @Mock
  private MediaProcessingService mediaProcessingService;

  @InjectMocks
  private MediaProcessedStreamConsumer consumer;

  private static final String STREAM_KEY = "media-processed-stream";

  @Nested
  @DisplayName("성공 메시지 처리")
  class SuccessMessage {

    @Test
    @DisplayName("UTC offset 포함 ISO-8601 processedAt을 LocalDateTime으로 정규화하여 applyProcessedResult를 호출한다")
    void onMessage_givenSuccessPayloadWithUtcOffset_callsApplyProcessedResult() {
      MapRecord<String, String, String> record = MapRecord.create(STREAM_KEY, Map.of(
          "mediaFileId", "1",
          "finalKey", "processed/42/abc.webp",
          "processedAt", "2026-05-27T14:32:52.160586+00:00"
      ));

      consumer.onMessage(record);

      verify(mediaProcessingService).applyProcessedResult(
          eq(1L),
          eq("processed/42/abc.webp"),
          eq(LocalDateTime.of(2026, 5, 27, 14, 32, 52, 160_586_000))
      );
      verify(mediaProcessingService, never()).applyFailedResult(any(), any());
    }

    @Test
    @DisplayName("processedAt이 잘못된 형식이어도 예외가 onMessage 밖으로 새지 않는다")
    void onMessage_givenMalformedProcessedAt_doesNotPropagateException() {
      MapRecord<String, String, String> record = MapRecord.create(STREAM_KEY, Map.of(
          "mediaFileId", "7",
          "finalKey", "processed/42/abc.webp",
          "processedAt", "not-a-timestamp"
      ));

      consumer.onMessage(record);

      verify(mediaProcessingService, never()).applyProcessedResult(any(), any(), any());
      verify(mediaProcessingService, never()).applyFailedResult(any(), any());
    }
  }

  @Nested
  @DisplayName("실패 메시지 처리")
  class FailureMessage {

    @Test
    @DisplayName("status=FAILED 필드를 포함한 메시지는 applyFailedResult를 호출한다")
    void onMessage_givenFailedPayload_callsApplyFailedResult() {
      MapRecord<String, String, String> record = MapRecord.create(STREAM_KEY, Map.of(
          "mediaFileId", "2",
          "status", "FAILED",
          "reason", "처리 타임아웃"
      ));

      consumer.onMessage(record);

      verify(mediaProcessingService).applyFailedResult(eq(2L), eq("처리 타임아웃"));
      verify(mediaProcessingService, never()).applyProcessedResult(any(), any(), any());
    }

    @Test
    @DisplayName("reason 필드 없는 실패 메시지는 'unknown'으로 fallback한다")
    void onMessage_givenFailedPayloadWithoutReason_usesUnknownFallback() {
      MapRecord<String, String, String> record = MapRecord.create(STREAM_KEY, Map.of(
          "mediaFileId", "3",
          "status", "FAILED"
      ));

      consumer.onMessage(record);

      verify(mediaProcessingService).applyFailedResult(eq(3L), eq("unknown"));
    }
  }

  @Nested
  @DisplayName("잘못된 페이로드 처리")
  class InvalidPayload {

    @Test
    @DisplayName("mediaFileId가 없는 메시지는 서비스를 호출하지 않고 예외를 던지지 않는다")
    void onMessage_givenMissingMediaFileId_doesNotCallServiceAndDoesNotThrow() {
      MapRecord<String, String, String> record = MapRecord.create(STREAM_KEY, Map.of(
          "finalKey", "processed/42/abc.webp",
          "processedAt", "2024-06-01T12:00:00"
      ));

      consumer.onMessage(record);

      verify(mediaProcessingService, never()).applyProcessedResult(any(), any(), any());
      verify(mediaProcessingService, never()).applyFailedResult(any(), any());
    }

    @Test
    @DisplayName("mediaFileId가 숫자가 아닌 메시지는 서비스를 호출하지 않고 예외를 던지지 않는다")
    void onMessage_givenNonNumericMediaFileId_doesNotCallServiceAndDoesNotThrow() {
      MapRecord<String, String, String> record = MapRecord.create(STREAM_KEY, Map.of(
          "mediaFileId", "not-a-number",
          "finalKey", "processed/42/abc.webp",
          "processedAt", "2024-06-01T12:00:00"
      ));

      consumer.onMessage(record);

      verify(mediaProcessingService, never()).applyProcessedResult(any(), any(), any());
      verify(mediaProcessingService, never()).applyFailedResult(any(), any());
    }

    @Test
    @DisplayName("서비스에서 예외가 발생해도 onMessage 밖으로 예외가 새지 않는다")
    void onMessage_givenServiceThrows_doesNotPropagateException() {
      MapRecord<String, String, String> record = MapRecord.create(STREAM_KEY, Map.of(
          "mediaFileId", "99",
          "finalKey", "processed/42/abc.webp",
          "processedAt", "2026-05-27T14:32:52.160586+00:00"
      ));

      org.mockito.Mockito.doThrow(new RuntimeException("DB 오류"))
          .when(mediaProcessingService)
          .applyProcessedResult(any(), any(), any());

      // 예외가 전파되지 않아야 함
      consumer.onMessage(record);
    }
  }
}
