package com.triptyche.backend.domain.notification.event;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.triptyche.backend.domain.device.service.DeviceService;
import com.triptyche.backend.domain.notification.dto.PushMessage;
import com.triptyche.backend.domain.notification.model.NotificationType;
import com.triptyche.backend.domain.notification.service.FcmSender;
import com.triptyche.backend.domain.notification.service.NotificationPushMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationPushListenerTest {

  private static final Long RECIPIENT_ID = 5L;
  private static final String LIVE_TOKEN = "LIVE_TOKEN";
  private static final String DEAD_TOKEN = "DEAD_TOKEN";

  @Mock
  private DeviceService deviceService;

  @Mock
  private FcmSender fcmSender;

  private NotificationPushListener listener;

  @BeforeEach
  void setUp() {
    listener = new NotificationPushListener(deviceService, new NotificationPushMapper(), fcmSender);
  }

  private NotificationSavedEvent event() {
    Map<String, Object> payload = new HashMap<>();
    payload.put("notificationId", 1L);
    payload.put("tripKey", "trip-1");
    payload.put("tripTitle", "뉴욕 가을 여행");
    payload.put("senderNickname", "홍길동");
    return new NotificationSavedEvent(RECIPIENT_ID, NotificationType.TRIP_UPDATED, payload);
  }

  @Nested
  @DisplayName("무효 토큰 정리")
  class InvalidTokenCleanup {

    @Test
    @DisplayName("FCM이 무효라고 답한 토큰만 삭제한다")
    void onNotificationSaved_givenInvalidTokens_removesOnlyThose() {
      // given
      given(deviceService.findTokens(RECIPIENT_ID)).willReturn(List.of(LIVE_TOKEN, DEAD_TOKEN));
      given(fcmSender.send(anyList(), any(PushMessage.class))).willReturn(List.of(DEAD_TOKEN));

      // when
      listener.onNotificationSaved(event());

      // then
      verify(deviceService).removeInvalidTokens(List.of(DEAD_TOKEN));
    }

    @Test
    @DisplayName("무효 토큰이 없으면 삭제할 것도 없다")
    void onNotificationSaved_givenAllTokensValid_removesNothing() {
      // given
      given(deviceService.findTokens(RECIPIENT_ID)).willReturn(List.of(LIVE_TOKEN));
      given(fcmSender.send(anyList(), any(PushMessage.class))).willReturn(List.of());

      // when
      listener.onNotificationSaved(event());

      // then
      verify(deviceService).removeInvalidTokens(List.of());
    }
  }

  @Nested
  @DisplayName("발송 조건")
  class Sending {

    @Test
    @DisplayName("등록된 기기가 없으면 발송하지 않는다")
    void onNotificationSaved_givenNoDevices_skipsSending() {
      // given
      given(deviceService.findTokens(RECIPIENT_ID)).willReturn(List.of());

      // when
      listener.onNotificationSaved(event());

      // then
      verify(fcmSender, never()).send(anyList(), any(PushMessage.class));
      verify(deviceService, never()).removeInvalidTokens(anyList());
    }

    @Test
    @DisplayName("발송이 실패해도 예외를 밖으로 던지지 않는다")
    void onNotificationSaved_givenSenderThrows_doesNotPropagate() {
      // given
      given(deviceService.findTokens(anyLong())).willReturn(List.of(LIVE_TOKEN));
      given(fcmSender.send(anyList(), any(PushMessage.class)))
              .willThrow(new RuntimeException("FCM down"));

      // when & then
      assertThatCode(() -> listener.onNotificationSaved(event())).doesNotThrowAnyException();
      verify(deviceService, never()).removeInvalidTokens(anyList());
    }
  }
}
