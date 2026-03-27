package com.triptyche.backend.domain.trip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import org.mockito.ArgumentCaptor;

import com.triptyche.backend.domain.trip.dto.TripCreationResponseDTO;
import com.triptyche.backend.domain.trip.dto.TripInfoRequestDTO;
import com.triptyche.backend.domain.trip.event.TripDeletedEvent;
import com.triptyche.backend.domain.trip.event.TripUpdatedEvent;
import com.triptyche.backend.domain.trip.model.Trip;
import com.triptyche.backend.domain.trip.model.TripStatus;
import com.triptyche.backend.domain.trip.repository.TripRepository;
import com.triptyche.backend.domain.trip.validator.TripAccessValidator;
import com.triptyche.backend.domain.user.model.User;
import com.triptyche.backend.global.common.ResultCode;
import com.triptyche.backend.global.exception.CustomException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class TripCommandServiceTest {

  @Mock
  private TripRepository tripRepository;

  @Mock
  private TripAccessValidator tripAccessValidator;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @InjectMocks
  private TripCommandService tripCommandService;

  private User owner;
  private User otherUser;

  private static final String TEST_TRIP_KEY = "TRIP_KEY_ABC";

  @BeforeEach
  void setUp() {
    owner = User.builder()
            .userId(1L)
            .userName("소유자")
            .userNickName("ownerNick")
            .userEmail("owner@example.com")
            .provider("google")
            .build();

    otherUser = User.builder()
            .userId(2L)
            .userName("타인")
            .userNickName("otherNick")
            .userEmail("other@example.com")
            .provider("google")
            .build();
  }

  private Trip createTrip(TripStatus status, User user) {
    return Trip.builder()
            .tripTitle("테스트 여행")
            .country("일본")
            .startDate(LocalDate.of(2024, 5, 1))
            .endDate(LocalDate.of(2024, 5, 7))
            .hashtags("여행,맛집")
            .status(status)
            .user(user)
            .build();
  }

  @Nested
  @DisplayName("createTripId()")
  class CreateTripId {

    @Test
    @DisplayName("인증된 사용자로 요청하면 DRAFT 상태의 여행이 저장되고 tripKey를 반환한다")
    void createTripId_givenAuthenticatedUser_returnsTripKey() {
      // given
      // tripRepository.save()는 void가 아닌 Trip을 반환하므로 저장된 Trip을 반환하도록 설정
      given(tripRepository.save(any(Trip.class))).willAnswer(invocation -> invocation.getArgument(0));

      // when
      TripCreationResponseDTO result = tripCommandService.createTripId(owner);

      // then
      // tripKey는 @PrePersist에서 생성되므로 실제 DB 없이는 null — 저장 호출 여부만 검증
      verify(tripRepository).save(any(Trip.class));
      assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("생성된 여행의 초기 상태는 DRAFT이다")
    void createTripId_givenUser_createdTripStatusIsDraft() {
      // given
      given(tripRepository.save(any(Trip.class))).willAnswer(invocation -> invocation.getArgument(0));

      // when
      tripCommandService.createTripId(owner);

      // then
      ArgumentCaptor<Trip> tripCaptor = ArgumentCaptor.forClass(Trip.class);
      verify(tripRepository).save(tripCaptor.capture());
      assertThat(tripCaptor.getValue().getStatus()).isEqualTo(TripStatus.DRAFT);
    }
  }

  @Nested
  @DisplayName("markImagesUploaded()")
  class MarkImagesUploaded {

    @Test
    @DisplayName("DRAFT 상태의 여행에 접근 가능한 사용자가 요청하면 상태가 IMAGES_UPLOADED로 변경된다")
    void markImagesUploaded_givenDraftTrip_statusChangesToImagesUploaded() {
      // given
      Trip trip = createTrip(TripStatus.DRAFT, owner);
      given(tripAccessValidator.validateAccessibleTripByKey(TEST_TRIP_KEY, owner)).willReturn(trip);

      // when
      tripCommandService.markImagesUploaded(owner, TEST_TRIP_KEY);

      // then
      assertThat(trip.getStatus()).isEqualTo(TripStatus.IMAGES_UPLOADED);
    }

    @Test
    @DisplayName("DRAFT가 아닌 상태의 여행에 요청하면 INVALID_TRIP_STATE 예외가 발생한다")
    void markImagesUploaded_givenNonDraftTrip_throwsInvalidTripState() {
      // given
      Trip trip = createTrip(TripStatus.IMAGES_UPLOADED, owner);
      given(tripAccessValidator.validateAccessibleTripByKey(TEST_TRIP_KEY, owner)).willReturn(trip);

      // when & then
      assertThatThrownBy(() -> tripCommandService.markImagesUploaded(owner, TEST_TRIP_KEY))
              .isInstanceOf(CustomException.class)
              .extracting(e -> ((CustomException) e).getResultCode())
              .isEqualTo(ResultCode.INVALID_TRIP_STATE);
    }

    @Test
    @DisplayName("접근 권한이 없는 사용자가 요청하면 UNAUTHORIZED_ACCESS 예외가 발생한다")
    void markImagesUploaded_givenUnauthorizedUser_throwsUnauthorizedAccess() {
      // given
      given(tripAccessValidator.validateAccessibleTripByKey(TEST_TRIP_KEY, otherUser))
              .willThrow(new CustomException(ResultCode.UNAUTHORIZED_ACCESS));

      // when & then
      assertThatThrownBy(() -> tripCommandService.markImagesUploaded(otherUser, TEST_TRIP_KEY))
              .isInstanceOf(CustomException.class)
              .extracting(e -> ((CustomException) e).getResultCode())
              .isEqualTo(ResultCode.UNAUTHORIZED_ACCESS);
    }
  }

  @Nested
  @DisplayName("finalizeTrip()")
  class FinalizeTrip {

    @Test
    @DisplayName("IMAGES_UPLOADED 상태의 여행에 요청하면 상태가 CONFIRMED로 변경된다")
    void finalizeTrip_givenImagesUploadedTrip_statusChangesToConfirmed() {
      // given
      Trip trip = createTrip(TripStatus.IMAGES_UPLOADED, owner);
      given(tripAccessValidator.validateAccessibleTripByKey(TEST_TRIP_KEY, owner)).willReturn(trip);

      // when
      tripCommandService.finalizeTrip(owner, TEST_TRIP_KEY);

      // then
      assertThat(trip.getStatus()).isEqualTo(TripStatus.CONFIRMED);
    }

    @Test
    @DisplayName("IMAGES_UPLOADED가 아닌 상태의 여행에 요청하면 INVALID_TRIP_STATE 예외가 발생한다")
    void finalizeTrip_givenNonImagesUploadedTrip_throwsInvalidTripState() {
      // given
      Trip trip = createTrip(TripStatus.DRAFT, owner);
      given(tripAccessValidator.validateAccessibleTripByKey(TEST_TRIP_KEY, owner)).willReturn(trip);

      // when & then
      assertThatThrownBy(() -> tripCommandService.finalizeTrip(owner, TEST_TRIP_KEY))
              .isInstanceOf(CustomException.class)
              .extracting(e -> ((CustomException) e).getResultCode())
              .isEqualTo(ResultCode.INVALID_TRIP_STATE);
    }

    @Test
    @DisplayName("접근 권한이 없는 사용자가 요청하면 UNAUTHORIZED_ACCESS 예외가 발생한다")
    void finalizeTrip_givenUnauthorizedUser_throwsUnauthorizedAccess() {
      // given
      given(tripAccessValidator.validateAccessibleTripByKey(TEST_TRIP_KEY, otherUser))
              .willThrow(new CustomException(ResultCode.UNAUTHORIZED_ACCESS));

      // when & then
      assertThatThrownBy(() -> tripCommandService.finalizeTrip(otherUser, TEST_TRIP_KEY))
              .isInstanceOf(CustomException.class)
              .extracting(e -> ((CustomException) e).getResultCode())
              .isEqualTo(ResultCode.UNAUTHORIZED_ACCESS);
    }
  }

  @Nested
  @DisplayName("updateTrip()")
  class UpdateTrip {

    private TripInfoRequestDTO updateRequest;

    @BeforeEach
    void setUp() {
      updateRequest = new TripInfoRequestDTO(
              "수정된 제목",
              "프랑스",
              LocalDate.of(2024, 6, 1),
              LocalDate.of(2024, 6, 10),
              List.of("파리", "여행")
      );
    }

    @Test
    @DisplayName("소유자가 여행 정보를 수정하면 Trip에 변경사항이 반영되고 isOwner=true인 이벤트가 발행된다")
    void updateTrip_givenOwner_updatesInfoAndPublishesEventWithIsOwnerTrue() {
      // given
      Trip trip = createTrip(TripStatus.CONFIRMED, owner);
      given(tripAccessValidator.validateAccessibleTripByKey(TEST_TRIP_KEY, owner)).willReturn(trip);

      // when
      tripCommandService.updateTrip(owner, TEST_TRIP_KEY, updateRequest);

      // then
      assertThat(trip.getTripTitle()).isEqualTo("수정된 제목");
      assertThat(trip.getCountry()).isEqualTo("프랑스");
      ArgumentCaptor<TripUpdatedEvent> captor = ArgumentCaptor.forClass(TripUpdatedEvent.class);
      verify(eventPublisher).publishEvent(captor.capture());
      assertThat(captor.getValue().isOwner()).isTrue();
    }

    @Test
    @DisplayName("공유된 사용자(비소유자)가 여행 정보를 수정하면 isOwner=false인 이벤트가 발행된다")
    void updateTrip_givenSharedUser_publishesEventWithIsOwnerFalse() {
      // given
      Trip trip = createTrip(TripStatus.CONFIRMED, owner);
      given(tripAccessValidator.validateAccessibleTripByKey(TEST_TRIP_KEY, otherUser)).willReturn(trip);

      // when
      tripCommandService.updateTrip(otherUser, TEST_TRIP_KEY, updateRequest);

      // then
      ArgumentCaptor<TripUpdatedEvent> captor = ArgumentCaptor.forClass(TripUpdatedEvent.class);
      verify(eventPublisher).publishEvent(captor.capture());
      assertThat(captor.getValue().isOwner()).isFalse();
    }

    @Test
    @DisplayName("접근 권한이 없는 사용자가 수정 요청하면 UNAUTHORIZED_ACCESS 예외가 발생한다")
    void updateTrip_givenUnauthorizedUser_throwsUnauthorizedAccess() {
      // given
      given(tripAccessValidator.validateAccessibleTripByKey(TEST_TRIP_KEY, otherUser))
              .willThrow(new CustomException(ResultCode.UNAUTHORIZED_ACCESS));

      // when & then
      assertThatThrownBy(() -> tripCommandService.updateTrip(otherUser, TEST_TRIP_KEY, updateRequest))
              .isInstanceOf(CustomException.class)
              .extracting(e -> ((CustomException) e).getResultCode())
              .isEqualTo(ResultCode.UNAUTHORIZED_ACCESS);
    }
  }

  @Nested
  @DisplayName("deleteTrip()")
  class DeleteTrip {

    @Test
    @DisplayName("소유자가 삭제 요청하면 여행이 소프트 삭제되고 TripDeletedEvent가 발행된다")
    void deleteTrip_givenOwner_softDeletesAndPublishesEvent() {
      // given
      Trip trip = createTrip(TripStatus.CONFIRMED, owner);
      given(tripAccessValidator.validateOwnerByKey(TEST_TRIP_KEY, owner)).willReturn(trip);

      // when
      tripCommandService.deleteTrip(owner, TEST_TRIP_KEY);

      // then
      assertThat(trip.isDeleted()).isTrue();
      verify(eventPublisher).publishEvent(any(TripDeletedEvent.class));
    }

    @Test
    @DisplayName("소유자가 아닌 사용자가 삭제 요청하면 UNAUTHORIZED_ACCESS 예외가 발생한다")
    void deleteTrip_givenNonOwner_throwsUnauthorizedAccess() {
      // given
      given(tripAccessValidator.validateOwnerByKey(TEST_TRIP_KEY, otherUser))
              .willThrow(new CustomException(ResultCode.UNAUTHORIZED_ACCESS));

      // when & then
      assertThatThrownBy(() -> tripCommandService.deleteTrip(otherUser, TEST_TRIP_KEY))
              .isInstanceOf(CustomException.class)
              .extracting(e -> ((CustomException) e).getResultCode())
              .isEqualTo(ResultCode.UNAUTHORIZED_ACCESS);
    }

    @Test
    @DisplayName("삭제 이벤트에 소유자 정보가 올바르게 담긴다")
    void deleteTrip_givenOwner_eventContainsOwnerInfo() {
      // given
      Trip trip = createTrip(TripStatus.CONFIRMED, owner);
      given(tripAccessValidator.validateOwnerByKey(TEST_TRIP_KEY, owner)).willReturn(trip);

      // when
      tripCommandService.deleteTrip(owner, TEST_TRIP_KEY);

      // then
      ArgumentCaptor<TripDeletedEvent> captor = ArgumentCaptor.forClass(TripDeletedEvent.class);
      verify(eventPublisher).publishEvent(captor.capture());
      TripDeletedEvent event = captor.getValue();
      assertThat(event.ownerId()).isEqualTo(owner.getUserId());
      assertThat(event.ownerNickname()).isEqualTo(owner.getUserNickName());
      assertThat(event.tripTitle()).isEqualTo("테스트 여행");
    }
  }
}
