package com.triptyche.backend.domain.share.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.triptyche.backend.domain.share.dto.ShareCreateRequest;
import com.triptyche.backend.domain.share.dto.ShareCreateResponse;
import com.triptyche.backend.domain.share.event.ShareApprovedEvent;
import com.triptyche.backend.domain.share.event.ShareRejectedEvent;
import com.triptyche.backend.domain.share.model.Share;
import com.triptyche.backend.domain.share.model.ShareStatus;
import com.triptyche.backend.domain.share.repository.ShareRepository;
import com.triptyche.backend.domain.trip.model.Trip;
import com.triptyche.backend.domain.trip.service.TripAccessGuard;
import com.triptyche.backend.domain.user.model.User;
import com.triptyche.backend.domain.user.repository.UserRepository;
import com.triptyche.backend.global.common.ResultCode;
import com.triptyche.backend.global.exception.CustomException;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class ShareServiceTest {

  @Mock
  private ShareRepository shareRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @Mock
  private TripAccessGuard tripAccessGuard;

  @InjectMocks
  private ShareService shareService;

  private User owner;
  private Trip trip;

  private static final String TEST_TRIP_KEY = "TRIP_KEY_ABC";
  private static final Long TRIP_ID = 1L;
  private static final Long OWNER_ID = 1L;
  private static final Long RECIPIENT_ID = 2L;

  @BeforeEach
  void setUp() {
    owner = User.builder()
            .userId(OWNER_ID)
            .userName("소유자")
            .userNickName("ownerNick")
            .userEmail("owner@example.com")
            .provider("google")
            .build();

    trip = Trip.builder()
            .tripId(TRIP_ID)
            .user(owner)
            .tripTitle("테스트 여행")
            .country("일본")
            .startDate(LocalDate.of(2024, 5, 1))
            .endDate(LocalDate.of(2024, 5, 7))
            .build();
  }

  @Nested
  @DisplayName("createShare()")
  class CreateShare {

    private ShareCreateRequest request;

    @BeforeEach
    void setUp() {
      request = new ShareCreateRequest(TEST_TRIP_KEY, RECIPIENT_ID);
      given(tripAccessGuard.validateAccessibleTripByKey(TEST_TRIP_KEY, owner)).willReturn(trip);
    }

    @Test
    @DisplayName("정상적인 공유 요청이면 Share가 저장되고 응답 DTO를 반환한다")
    void createShare_givenValidRequest_savesAndReturnsResponse() {
      // given
      given(shareRepository.existsByTripAndRecipientId(trip, RECIPIENT_ID)).willReturn(false);
      Share savedShare = Share.builder()
              .shareId(1L)
              .trip(trip)
              .recipientId(RECIPIENT_ID)
              .shareStatus(ShareStatus.PENDING)
              .build();
      given(shareRepository.save(any(Share.class))).willReturn(savedShare);

      // when
      ShareCreateResponse result = shareService.createShare(request, owner);

      // then
      verify(shareRepository).save(any(Share.class));
      assertThat(result.shareId()).isEqualTo(1L);
      assertThat(result.recipientId()).isEqualTo(RECIPIENT_ID);
      assertThat(result.shareStatus()).isEqualTo(ShareStatus.PENDING);
    }

    @Test
    @DisplayName("자기 자신에게 공유 요청하면 CANNOT_SHARE_TO_SELF 예외가 발생한다")
    void createShare_givenSelfRecipient_throwsCannotShareToSelf() {
      // given
      ShareCreateRequest selfRequest = new ShareCreateRequest(TEST_TRIP_KEY, OWNER_ID);

      // when & then
      assertThatThrownBy(() -> shareService.createShare(selfRequest, owner))
              .isInstanceOf(CustomException.class)
              .extracting(e -> ((CustomException) e).getResultCode())
              .isEqualTo(ResultCode.CANNOT_SHARE_TO_SELF);
    }

    @Test
    @DisplayName("이미 공유된 여행에 요청하면 SHARE_ALREADY_EXIST 예외가 발생한다")
    void createShare_givenAlreadyShared_throwsShareAlreadyExist() {
      // given
      given(shareRepository.existsByTripAndRecipientId(trip, RECIPIENT_ID)).willReturn(true);

      // when & then
      assertThatThrownBy(() -> shareService.createShare(request, owner))
              .isInstanceOf(CustomException.class)
              .extracting(e -> ((CustomException) e).getResultCode())
              .isEqualTo(ResultCode.SHARE_ALREADY_EXIST);
    }

    @Test
    @DisplayName("동시 요청으로 DataIntegrityViolationException 발생 시 DUPLICATE_DATA_CONFLICT 예외로 변환된다")
    void createShare_givenConcurrentRequest_throwsDuplicateDataConflict() {
      // given
      // existsByTripAndRecipientId 체크는 통과했지만 save 시점에 유니크 제약조건 위반
      given(shareRepository.existsByTripAndRecipientId(trip, RECIPIENT_ID)).willReturn(false);
      given(shareRepository.save(any(Share.class))).willThrow(DataIntegrityViolationException.class);

      // when & then
      assertThatThrownBy(() -> shareService.createShare(request, owner))
              .isInstanceOf(CustomException.class)
              .extracting(e -> ((CustomException) e).getResultCode())
              .isEqualTo(ResultCode.DUPLICATE_DATA_CONFLICT);
    }
  }

  @Nested
  @DisplayName("updateShareStatus()")
  class UpdateShareStatus {

    private static final Long SHARE_ID = 10L;
    private User recipient;

    @BeforeEach
    void setUp() {
      recipient = User.builder()
              .userId(RECIPIENT_ID)
              .userName("수신자")
              .userNickName("recipientNick")
              .userEmail("recipient@example.com")
              .provider("google")
              .build();
    }

    private Share buildShare(ShareStatus status) {
      return Share.builder()
              .shareId(SHARE_ID)
              .trip(trip)
              .recipientId(RECIPIENT_ID)
              .shareStatus(status)
              .build();
    }

    @Test
    @DisplayName("PENDING 상태의 공유 요청을 APPROVED로 변경하면 상태가 변경되고 ShareApprovedEvent가 발행된다")
    void updateShareStatus_givenPendingShare_approvesAndPublishesEvent() {
      // given
      Share share = buildShare(ShareStatus.PENDING);
      given(shareRepository.findByIdWithTripAndOwner(SHARE_ID)).willReturn(Optional.of(share));
      given(userRepository.findById(RECIPIENT_ID)).willReturn(Optional.of(recipient));

      // when
      shareService.updateShareStatus(SHARE_ID, ShareStatus.APPROVED);

      // then
      assertThat(share.getShareStatus()).isEqualTo(ShareStatus.APPROVED);
      verify(eventPublisher).publishEvent(any(ShareApprovedEvent.class));
    }

    @Test
    @DisplayName("PENDING 상태의 공유 요청을 REJECTED로 변경하면 상태가 변경되고 ShareRejectedEvent가 발행된다")
    void updateShareStatus_givenPendingShare_rejectsAndPublishesEvent() {
      // given
      Share share = buildShare(ShareStatus.PENDING);
      given(shareRepository.findByIdWithTripAndOwner(SHARE_ID)).willReturn(Optional.of(share));
      given(userRepository.findById(RECIPIENT_ID)).willReturn(Optional.of(recipient));

      // when
      shareService.updateShareStatus(SHARE_ID, ShareStatus.REJECTED);

      // then
      assertThat(share.getShareStatus()).isEqualTo(ShareStatus.REJECTED);
      verify(eventPublisher).publishEvent(any(ShareRejectedEvent.class));
    }

    @Test
    @DisplayName("PENDING이 아닌 상태의 공유 요청 변경 시 INVALID_SHARE_STATUS_TRANSITION 예외가 발생한다")
    void updateShareStatus_givenNonPendingShare_throwsInvalidShareStatusTransition() {
      // given
      Share share = buildShare(ShareStatus.APPROVED);
      given(shareRepository.findByIdWithTripAndOwner(SHARE_ID)).willReturn(Optional.of(share));

      // when & then
      assertThatThrownBy(() -> shareService.updateShareStatus(SHARE_ID, ShareStatus.REJECTED))
              .isInstanceOf(CustomException.class)
              .extracting(e -> ((CustomException) e).getResultCode())
              .isEqualTo(ResultCode.INVALID_SHARE_STATUS_TRANSITION);
    }
  }

}
