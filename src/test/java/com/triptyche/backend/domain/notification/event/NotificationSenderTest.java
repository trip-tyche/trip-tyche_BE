package com.triptyche.backend.domain.notification.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

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
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class NotificationSenderTest {

  @Mock
  private NotificationRepository notificationRepository;

  @Mock
  private ApplicationEventPublisher eventPublisher;

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
    void sendNotification_givenValidParams_savesNotificationWithUnreadStatus() {
      ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);

      notificationSender.sendNotification(RECIPIENT_ID, TYPE, PAYLOAD, REFERENCE_ID, SENDER_NICKNAME);

      verify(notificationRepository).save(captor.capture());
      Notification saved = captor.getValue();
      assertThat(saved.getUserId()).isEqualTo(RECIPIENT_ID);
      assertThat(saved.getMessage()).isEqualTo(TYPE);
      assertThat(saved.getStatus()).isEqualTo(NotificationStatus.UNREAD);
      assertThat(saved.getReferenceId()).isEqualTo(REFERENCE_ID);
      assertThat(saved.getSenderNickname()).isEqualTo(SENDER_NICKNAME);
    }

    @Test
    @DisplayName("sendNotification() 호출 시 NotificationSavedEvent가 올바른 필드로 발행된다")
    void sendNotification_givenValidParams_publishesNotificationSavedEventWithCorrectFields() {
      ArgumentCaptor<NotificationSavedEvent> captor = ArgumentCaptor.forClass(NotificationSavedEvent.class);

      notificationSender.sendNotification(RECIPIENT_ID, TYPE, PAYLOAD, REFERENCE_ID, SENDER_NICKNAME);

      verify(eventPublisher).publishEvent(captor.capture());
      NotificationSavedEvent event = captor.getValue();
      assertThat(event.recipientId()).isEqualTo(RECIPIENT_ID);
      assertThat(event.type()).isEqualTo(TYPE);
      assertThat(event.payload()).isEqualTo(PAYLOAD);
    }

    @Test
    @DisplayName("알림 저장 후 이벤트가 발행되어 DB 저장과 WebSocket 송신이 분리된다")
    void sendNotification_givenValidParams_savesNotificationAndPublishesEvent() {
      notificationSender.sendNotification(RECIPIENT_ID, TYPE, PAYLOAD, REFERENCE_ID, SENDER_NICKNAME);

      verify(notificationRepository).save(any(Notification.class));
      verify(eventPublisher).publishEvent(any(NotificationSavedEvent.class));
    }
  }
}