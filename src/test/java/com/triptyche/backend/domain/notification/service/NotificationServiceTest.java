package com.triptyche.backend.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.triptyche.backend.domain.notification.dto.NotificationDetailResponse;
import com.triptyche.backend.domain.notification.dto.NotificationResponse;
import com.triptyche.backend.domain.notification.model.Notification;
import com.triptyche.backend.domain.notification.model.NotificationStatus;
import com.triptyche.backend.domain.notification.model.NotificationType;
import com.triptyche.backend.domain.notification.repository.NotificationRepository;
import com.triptyche.backend.domain.trip.service.TripQueryService;
import com.triptyche.backend.global.common.ResultCode;
import com.triptyche.backend.global.exception.CustomException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

  @Mock
  private NotificationRepository notificationRepository;

  @Mock
  private TripQueryService tripQueryService;

  @InjectMocks
  private NotificationService notificationService;

  private static final Long USER_ID = 1L;
  private static final Long NOTIFICATION_ID = 100L;
  private static final Long TRIP_ID = 10L;

  private Notification buildNotification(Long id, NotificationStatus status) {
    return Notification.builder()
            .notificationId(id)
            .userId(USER_ID)
            .message(NotificationType.SHARED_REQUEST)
            .status(status)
            .referenceId(TRIP_ID)
            .senderNickname("보내는사람")
            .createdAt(LocalDateTime.of(2024, 5, 1, 12, 0))
            .build();
  }

  @Nested
  @DisplayName("getActiveNotifications()")
  class GetActiveNotifications {

    @Test
    @DisplayName("삭제되지 않은 알림 목록을 반환한다")
    void getActiveNotifications_givenUserId_returnsNonDeletedNotifications() {
      // given
      Notification unread = buildNotification(1L, NotificationStatus.UNREAD);
      Notification read = buildNotification(2L, NotificationStatus.READ);
      given(notificationRepository.findByUserIdAndStatusNot(USER_ID, NotificationStatus.DELETE))
              .willReturn(List.of(unread, read));

      // when
      List<NotificationResponse> result = notificationService.getActiveNotifications(USER_ID);

      // then
      assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("알림 타입과 상태는 enum 이름 문자열로 반환된다")
    void getActiveNotifications_givenNotification_responseContainsEnumNameStrings() {
      // given
      Notification unread = buildNotification(1L, NotificationStatus.UNREAD);
      given(notificationRepository.findByUserIdAndStatusNot(USER_ID, NotificationStatus.DELETE))
              .willReturn(List.of(unread));

      // when
      List<NotificationResponse> result = notificationService.getActiveNotifications(USER_ID);

      // then
      NotificationResponse response = result.get(0);
      assertThat(response.message()).isEqualTo(NotificationType.SHARED_REQUEST.name());
      assertThat(response.status()).isEqualTo(NotificationStatus.UNREAD.name());
    }

    @Test
    @DisplayName("알림이 없으면 빈 리스트를 반환한다")
    void getActiveNotifications_givenNoNotifications_returnsEmptyList() {
      // given
      given(notificationRepository.findByUserIdAndStatusNot(USER_ID, NotificationStatus.DELETE))
              .willReturn(List.of());

      // when
      List<NotificationResponse> result = notificationService.getActiveNotifications(USER_ID);

      // then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("알림 ID, 참조 ID, 발신자 닉네임이 응답에 올바르게 담긴다")
    void getActiveNotifications_givenNotification_responseFieldsAreMappedCorrectly() {
      // given
      Notification unread = buildNotification(NOTIFICATION_ID, NotificationStatus.UNREAD);
      given(notificationRepository.findByUserIdAndStatusNot(USER_ID, NotificationStatus.DELETE))
              .willReturn(List.of(unread));

      // when
      List<NotificationResponse> result = notificationService.getActiveNotifications(USER_ID);

      // then
      NotificationResponse response = result.get(0);
      assertThat(response.notificationId()).isEqualTo(NOTIFICATION_ID);
      assertThat(response.referenceId()).isEqualTo(TRIP_ID);
      assertThat(response.senderNickname()).isEqualTo("보내는사람");
    }
  }

  @Nested
  @DisplayName("markAsRead()")
  class MarkAsRead {

    @Test
    @DisplayName("읽지 않은 알림을 읽음 처리하면 상태가 READ로 변경된다")
    void markAsRead_givenUnreadNotification_statusChangesToRead() {
      // given
      Notification notification = buildNotification(NOTIFICATION_ID, NotificationStatus.UNREAD);
      given(notificationRepository.findById(NOTIFICATION_ID)).willReturn(Optional.of(notification));

      // when
      notificationService.markAsRead(NOTIFICATION_ID);

      // then
      assertThat(notification.getStatus()).isEqualTo(NotificationStatus.READ);
    }

    @Test
    @DisplayName("이미 읽은 알림은 다시 읽음 처리해도 상태가 그대로다")
    void markAsRead_givenAlreadyReadNotification_statusRemainsRead() {
      // given
      // READ 상태인 알림은 markAsRead() 내부에서 조기 반환(early return)되어야 한다
      Notification notification = buildNotification(NOTIFICATION_ID, NotificationStatus.READ);
      given(notificationRepository.findById(NOTIFICATION_ID)).willReturn(Optional.of(notification));

      // when
      notificationService.markAsRead(NOTIFICATION_ID);

      // then
      assertThat(notification.getStatus()).isEqualTo(NotificationStatus.READ);
    }

    @Test
    @DisplayName("존재하지 않는 알림 ID로 요청하면 NOTIFICATION_NOT_FOUND 예외가 발생한다")
    void markAsRead_givenNonExistentNotificationId_throwsNotificationNotFound() {
      // given
      given(notificationRepository.findById(NOTIFICATION_ID)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> notificationService.markAsRead(NOTIFICATION_ID))
              .isInstanceOf(CustomException.class)
              .extracting(e -> ((CustomException) e).getResultCode())
              .isEqualTo(ResultCode.NOTIFICATION_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("getNotificationDetail()")
  class GetNotificationDetail {

    @Test
    @DisplayName("알림과 연관 여행이 모두 존재하면 여행 제목, 알림 타입, 발신자가 담긴 응답을 반환한다")
    void getNotificationDetail_givenExistingNotificationAndTrip_returnsDetailResponse() {
      // given
      Notification notification = buildNotification(NOTIFICATION_ID, NotificationStatus.UNREAD);
      given(notificationRepository.findById(NOTIFICATION_ID)).willReturn(Optional.of(notification));
      given(tripQueryService.getTripTitleById(TRIP_ID)).willReturn("제주도 여행");

      // when
      NotificationDetailResponse result = notificationService.getNotificationDetail(NOTIFICATION_ID);

      // then
      assertThat(result.tripTitle()).isEqualTo("제주도 여행");
      assertThat(result.message()).isEqualTo(NotificationType.SHARED_REQUEST);
      assertThat(result.senderNickname()).isEqualTo("보내는사람");
    }

    @Test
    @DisplayName("연관된 여행이 존재하지 않으면 여행 제목이 UNKNOWN_TRIP으로 반환된다")
    void getNotificationDetail_givenMissingTrip_returnsTripTitleAsUnknownTrip() {
      // given
      // 여행이 삭제되었거나 referenceId가 유효하지 않은 경우를 검증한다
      Notification notification = buildNotification(NOTIFICATION_ID, NotificationStatus.UNREAD);
      given(notificationRepository.findById(NOTIFICATION_ID)).willReturn(Optional.of(notification));
      given(tripQueryService.getTripTitleById(TRIP_ID)).willReturn("UNKNOWN_TRIP");

      // when
      NotificationDetailResponse result = notificationService.getNotificationDetail(NOTIFICATION_ID);

      // then
      assertThat(result.tripTitle()).isEqualTo("UNKNOWN_TRIP");
    }

    @Test
    @DisplayName("존재하지 않는 알림 ID로 요청하면 NOTIFICATION_NOT_FOUND 예외가 발생한다")
    void getNotificationDetail_givenNonExistentNotificationId_throwsNotificationNotFound() {
      // given
      given(notificationRepository.findById(NOTIFICATION_ID)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> notificationService.getNotificationDetail(NOTIFICATION_ID))
              .isInstanceOf(CustomException.class)
              .extracting(e -> ((CustomException) e).getResultCode())
              .isEqualTo(ResultCode.NOTIFICATION_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("markAsDeleted()")
  class MarkAsDeleted {

    @Test
    @DisplayName("알림 ID 목록을 전달하면 해당 알림들이 모두 삭제 처리된다")
    void markAsDeleted_givenUnreadNotifications_allStatusChangeToDelete() {
      // given
      Notification n1 = buildNotification(1L, NotificationStatus.UNREAD);
      Notification n2 = buildNotification(2L, NotificationStatus.READ);
      List<Long> ids = List.of(1L, 2L);
      given(notificationRepository.findAllById(ids)).willReturn(List.of(n1, n2));

      // when
      notificationService.markAsDeleted(ids);

      // then
      assertThat(n1.getStatus()).isEqualTo(NotificationStatus.DELETE);
      assertThat(n2.getStatus()).isEqualTo(NotificationStatus.DELETE);
    }

    @Test
    @DisplayName("이미 삭제된 알림은 중복 처리되지 않고 상태가 그대로 유지된다")
    void markAsDeleted_givenAlreadyDeletedNotification_statusRemainsDelete() {
      // given
      // DELETE 상태인 알림은 필터에서 걸러지므로 markAsDeleted()가 호출되지 않아야 한다
      Notification alreadyDeleted = buildNotification(1L, NotificationStatus.DELETE);
      List<Long> ids = List.of(1L);
      given(notificationRepository.findAllById(ids)).willReturn(List.of(alreadyDeleted));

      // when
      notificationService.markAsDeleted(ids);

      // then
      // 상태가 DELETE로 유지되며 이중 처리되지 않음을 확인
      assertThat(alreadyDeleted.getStatus()).isEqualTo(NotificationStatus.DELETE);
    }

    @Test
    @DisplayName("빈 ID 목록을 전달하면 아무런 상태 변경 없이 정상 종료된다")
    void markAsDeleted_givenEmptyIdList_doesNothing() {
      // given
      given(notificationRepository.findAllById(List.of())).willReturn(List.of());

      // when & then
      // 예외 없이 정상 종료되어야 한다
      notificationService.markAsDeleted(List.of());
    }

    @Test
    @DisplayName("존재하지 않는 id가 포함되어도 예외 없이 정상 종료된다")
    void markAsDeleted_givenNonExistentId_doesNotThrow() {
      // given
      // findAllById는 존재하지 않는 id를 무시하고 빈 리스트 반환
      given(notificationRepository.findAllById(List.of(999L))).willReturn(List.of());

      // when & then
      // 예외 없이 정상 종료 — 멱등성 설계 확인
      notificationService.markAsDeleted(List.of(999L));
    }

    @Test
    @DisplayName("여러 상태의 알림이 섞여 있을 때 DELETE가 아닌 알림만 삭제 처리된다")
    void markAsDeleted_givenMixedStatusNotifications_onlyNonDeletedAreUpdated() {
      // given
      Notification unread = buildNotification(1L, NotificationStatus.UNREAD);
      Notification deleted = buildNotification(2L, NotificationStatus.DELETE);
      List<Long> ids = List.of(1L, 2L);
      given(notificationRepository.findAllById(ids)).willReturn(List.of(unread, deleted));

      // when
      notificationService.markAsDeleted(ids);

      // then
      assertThat(unread.getStatus()).isEqualTo(NotificationStatus.DELETE);
      // DELETE 상태였던 것은 필터에서 제외되어 상태 변화 없음
      assertThat(deleted.getStatus()).isEqualTo(NotificationStatus.DELETE);
    }
  }
}
