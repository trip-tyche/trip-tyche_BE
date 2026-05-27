package com.triptyche.backend.domain.media.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.event.TransactionalEventListener;

@ExtendWith(MockitoExtension.class)
class ImageProcessedEventListenerTest {

  @Mock
  private SimpMessagingTemplate messagingTemplate;

  @InjectMocks
  private ImageProcessedEventListener listener;

  private static final Long MEDIA_FILE_ID = 1L;
  private static final Long USER_ID = 42L;
  private static final String FINAL_URL = "https://cdn.example.com/image.webp";
  private static final LocalDateTime PROCESSED_AT = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
  private static final String REASON = "conversion failed";
  private static final String EXPECTED_TOPIC = "/topic/media-processed/42";

  @Nested
  @DisplayName("onImageProcessed()")
  class OnImageProcessed {

    @Test
    @DisplayName("PROCESSED payload를 올바른 토픽으로 전송한다")
    void onImageProcessed_sendsCorrectPayloadToTopic() {
      ImageProcessedEvent event = new ImageProcessedEvent(MEDIA_FILE_ID, USER_ID, FINAL_URL, PROCESSED_AT);

      listener.onImageProcessed(event);

      @SuppressWarnings("unchecked")
      ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
      verify(messagingTemplate).convertAndSend(eq(EXPECTED_TOPIC), captor.capture());

      Map<String, Object> payload = captor.getValue();
      assertThat(payload).containsEntry("mediaFileId", MEDIA_FILE_ID);
      assertThat(payload).containsEntry("finalUrl", FINAL_URL);
      assertThat(payload).containsEntry("status", "PROCESSED");
      assertThat(payload).containsEntry("processedAt", PROCESSED_AT.toString());
    }

    @Test
    @DisplayName("messagingTemplate이 예외를 던져도 listener는 예외를 전파하지 않는다")
    void onImageProcessed_whenSendThrows_doesNotPropagate() {
      ImageProcessedEvent event = new ImageProcessedEvent(MEDIA_FILE_ID, USER_ID, FINAL_URL, PROCESSED_AT);
      doThrow(new RuntimeException("STOMP error")).when(messagingTemplate)
          .convertAndSend(eq(EXPECTED_TOPIC), any(Map.class));

      assertThatCode(() -> listener.onImageProcessed(event)).doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("onImageProcessingFailed()")
  class OnImageProcessingFailed {

    @Test
    @DisplayName("FAILED payload를 올바른 토픽으로 전송한다")
    void onImageProcessingFailed_sendsCorrectPayloadToTopic() {
      ImageProcessingFailedEvent event = new ImageProcessingFailedEvent(MEDIA_FILE_ID, USER_ID, REASON);

      listener.onImageProcessingFailed(event);

      @SuppressWarnings("unchecked")
      ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
      verify(messagingTemplate).convertAndSend(eq(EXPECTED_TOPIC), captor.capture());

      Map<String, Object> payload = captor.getValue();
      assertThat(payload).containsEntry("mediaFileId", MEDIA_FILE_ID);
      assertThat(payload).containsEntry("status", "FAILED");
      assertThat(payload).containsEntry("reason", REASON);
    }

    @Test
    @DisplayName("messagingTemplate이 예외를 던져도 listener는 예외를 전파하지 않는다")
    void onImageProcessingFailed_whenSendThrows_doesNotPropagate() {
      ImageProcessingFailedEvent event = new ImageProcessingFailedEvent(MEDIA_FILE_ID, USER_ID, REASON);
      doThrow(new RuntimeException("STOMP error")).when(messagingTemplate)
          .convertAndSend(eq(EXPECTED_TOPIC), any(Map.class));

      assertThatCode(() -> listener.onImageProcessingFailed(event)).doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("@TransactionalEventListener 어노테이션 검증")
  class AnnotationVerification {

    @Test
    @DisplayName("onImageProcessed에 @TransactionalEventListener가 선언되어 있다")
    void onImageProcessed_hasTransactionalEventListenerAnnotation() throws NoSuchMethodException {
      Method method = ImageProcessedEventListener.class.getMethod("onImageProcessed", ImageProcessedEvent.class);
      assertThat(method.isAnnotationPresent(TransactionalEventListener.class)).isTrue();
    }

    @Test
    @DisplayName("onImageProcessingFailed에 @TransactionalEventListener가 선언되어 있다")
    void onImageProcessingFailed_hasTransactionalEventListenerAnnotation() throws NoSuchMethodException {
      Method method = ImageProcessedEventListener.class.getMethod("onImageProcessingFailed", ImageProcessingFailedEvent.class);
      assertThat(method.isAnnotationPresent(TransactionalEventListener.class)).isTrue();
    }
  }
}
