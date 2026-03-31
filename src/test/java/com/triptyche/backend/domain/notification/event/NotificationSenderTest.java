package com.triptyche.backend.domain.notification.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.triptyche.backend.domain.notification.model.Notification;
import com.triptyche.backend.domain.notification.model.NotificationStatus;
import com.triptyche.backend.domain.notification.model.NotificationType;
import com.triptyche.backend.domain.notification.repository.NotificationRepository;
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

@ExtendWith(MockitoExtension.class)
class NotificationSenderTest {

  @Mock
  private NotificationRepository notificationRepository;

  @Mock
  private SimpMessagingTemplate messagingTemplate;

  @Mock
  private ObjectMapper objectMapper;

  @InjectMocks
  private NotificationSender notificationSender;

  private static final Long RECIPIENT_ID = 1L;
  private static final Long REFERENCE_ID = 10L;
  private static final String SENDER_NICKNAME = "보내는사람";
  private static final NotificationType TYPE = NotificationType.SHARED_REQUEST;
  private static final Map<String, Object> PAYLOAD = Map.of("type", TYPE.name());

  @Nested
  @DisplayName("sendNotification()")
  class SendNotification {

    @Test
    @DisplayName("sendNotification() 호출 시 Notification이 UNREAD 상태로 저장된다")
    void sendNotification_givenValidParams_savesNotificationWithUnreadStatus()
            throws JsonProcessingException {
      // given
      given(objectMapper.writeValueAsString(any())).willReturn("{}");
      ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);

      // when
      notificationSender.sendNotification(RECIPIENT_ID, TYPE, PAYLOAD, REFERENCE_ID, SENDER_NICKNAME);

      // then
      verify(notificationRepository).save(captor.capture());
      Notification saved = captor.getValue();
      assertThat(saved.getUserId()).isEqualTo(RECIPIENT_ID);
      assertThat(saved.getMessage()).isEqualTo(TYPE);
      assertThat(saved.getStatus()).isEqualTo(NotificationStatus.UNREAD);
      assertThat(saved.getReferenceId()).isEqualTo(REFERENCE_ID);
      assertThat(saved.getSenderNickname()).isEqualTo(SENDER_NICKNAME);
    }

    @Test
    @DisplayName("sendNotification() 호출 시 올바른 WebSocket 경로로 메시지가 전송된다")
    void sendNotification_givenValidParams_sendsToCorrectWebSocketTopic()
            throws JsonProcessingException {
      // given
      given(objectMapper.writeValueAsString(any())).willReturn("{}");

      // when
      notificationSender.sendNotification(RECIPIENT_ID, TYPE, PAYLOAD, REFERENCE_ID, SENDER_NICKNAME);

      // then
      verify(messagingTemplate).convertAndSend(
              eq("/topic/share-notifications/" + RECIPIENT_ID),
              eq("{}")
      );
    }

    @Test
    @DisplayName("WebSocket 전송 중 예외가 발생해도 외부로 전파되지 않는다")
    void sendNotification_whenWebSocketFails_doesNotPropagateException()
            throws JsonProcessingException {
      // given
      given(objectMapper.writeValueAsString(any()))
              .willThrow(new JsonProcessingException("test") {});

      // when & then
      // 예외가 외부로 전파되지 않아야 한다
      assertThatCode(() ->
              notificationSender.sendNotification(RECIPIENT_ID, TYPE, PAYLOAD, REFERENCE_ID, SENDER_NICKNAME)
      ).doesNotThrowAnyException();

      // DB 저장은 WebSocket 실패와 무관하게 정상 호출됐어야 한다
      verify(notificationRepository).save(any(Notification.class));
    }
  }
}
