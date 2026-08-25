package com.triptyche.backend.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.triptyche.backend.domain.notification.dto.PushMessage;
import com.triptyche.backend.domain.notification.model.NotificationType;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class NotificationPushMapperTest {

  private static final Long NOTIFICATION_ID = 77L;
  private static final String TRIP_KEY = "trip-abc-123";

  private final NotificationPushMapper mapper = new NotificationPushMapper();

  private Map<String, Object> payload(boolean withTripKey) {
    Map<String, Object> payload = new HashMap<>();
    payload.put("notificationId", NOTIFICATION_ID);
    payload.put("recipientId", 5L);
    payload.put("shareId", 9L);
    payload.put("senderNickname", "홍길동");
    payload.put("tripTitle", "뉴욕 가을 여행");
    payload.put("count", 3);

    if (withTripKey) {
      payload.put("tripKey", TRIP_KEY);
    }
    return payload;
  }

  @Nested
  @DisplayName("8개 타입 전체")
  class AllTypes {

    @ParameterizedTest
    @EnumSource(NotificationType.class)
    @DisplayName("모든 타입이 제목과 본문을 만든다")
    void toPushMessage_givenAnyType_producesTitleAndBody(NotificationType type) {
      // when
      PushMessage message = mapper.toPushMessage(type, payload(true));

      // then
      assertThat(message.title()).isNotBlank();
      assertThat(message.body()).isNotBlank();
      assertThat(message.type()).isEqualTo(type.name());
    }

    @ParameterizedTest
    @EnumSource(NotificationType.class)
    @DisplayName("FCM data에 실릴 값은 모두 문자열이다")
    void toPushMessage_givenAnyType_producesStringOnlyFields(NotificationType type) {
      // when
      PushMessage message = mapper.toPushMessage(type, payload(true));

      // then
      assertThat(message.type()).isInstanceOf(String.class);
      assertThat(message.resourceId()).isInstanceOf(String.class);
      assertThat(message.deeplink()).isInstanceOf(String.class);
    }
  }

  @Nested
  @DisplayName("딥링크")
  class Deeplink {

    @Test
    @DisplayName("tripKey가 있으면 여행 상세로 보낸다")
    void toPushMessage_givenTripKey_buildsTripDeeplink() {
      // when
      PushMessage message = mapper.toPushMessage(NotificationType.TRIP_UPDATED, payload(true));

      // then
      assertThat(message.deeplink()).isEqualTo("triptyche://trip/" + TRIP_KEY);
      assertThat(message.resourceId()).isEqualTo(TRIP_KEY);
    }

    @ParameterizedTest
    @EnumSource(value = NotificationType.class,
            names = {"SHARED_REQUEST", "SHARED_REJECTED", "TRIP_DELETED"})
    @DisplayName("tripKey가 없는 타입은 딥링크 없이 notificationId를 싣는다")
    void toPushMessage_givenNoTripKey_omitsDeeplink(NotificationType type) {
      // when
      PushMessage message = mapper.toPushMessage(type, payload(false));

      // then
      assertThat(message.deeplink()).isNull();
      assertThat(message.resourceId()).isEqualTo(String.valueOf(NOTIFICATION_ID));
    }
  }

  @Nested
  @DisplayName("본문 구성")
  class Body {

    @Test
    @DisplayName("사진 추가는 장수를 본문에 넣는다")
    void toPushMessage_givenMediaAdded_includesCount() {
      // when
      PushMessage message = mapper.toPushMessage(NotificationType.MEDIA_FILE_ADDED, payload(true));

      // then
      assertThat(message.body()).contains("홍길동").contains("뉴욕 가을 여행").contains("3장");
    }

    @Test
    @DisplayName("장수가 0이면 숫자를 붙이지 않는다")
    void toPushMessage_givenZeroCount_omitsAmount() {
      // given
      Map<String, Object> payload = payload(true);
      payload.put("count", 0);

      // when
      PushMessage message = mapper.toPushMessage(NotificationType.MEDIA_FILE_ADDED, payload);

      // then
      assertThat(message.body()).doesNotContain("0장");
    }

    @Test
    @DisplayName("보낸 사람과 여행 제목이 비어 있어도 본문을 만든다")
    void toPushMessage_givenMissingFields_stillProducesBody() {
      // given
      Map<String, Object> payload = new HashMap<>();
      payload.put("notificationId", NOTIFICATION_ID);

      // when
      PushMessage message = mapper.toPushMessage(NotificationType.SHARED_REQUEST, payload);

      // then
      assertThat(message.body()).isNotBlank();
      assertThat(message.resourceId()).isEqualTo(String.valueOf(NOTIFICATION_ID));
    }
  }
}
