package com.triptyche.backend.domain.share.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.triptyche.backend.domain.share.dto.ShareCreateRequestDTO;
import com.triptyche.backend.domain.share.dto.ShareCreateResponseDTO;
import com.triptyche.backend.domain.share.model.Share;
import com.triptyche.backend.domain.share.model.ShareStatus;
import com.triptyche.backend.domain.share.repository.ShareRepository;
import com.triptyche.backend.domain.trip.converter.TripKeyConverter;
import com.triptyche.backend.domain.trip.model.Trip;
import com.triptyche.backend.domain.trip.validator.TripAccessValidator;
import com.triptyche.backend.domain.user.model.User;
import com.triptyche.backend.domain.user.repository.UserRepository;
import com.triptyche.backend.global.common.ResultCode;
import com.triptyche.backend.global.exception.CustomException;
import java.time.LocalDate;
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
  private TripKeyConverter tripKeyConverter;

  @Mock
  private TripAccessValidator tripAccessValidator;

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

    private ShareCreateRequestDTO request;

    @BeforeEach
    void setUp() {
      request = new ShareCreateRequestDTO(TEST_TRIP_KEY, RECIPIENT_ID);
      given(tripKeyConverter.convertToTripId(TEST_TRIP_KEY)).willReturn(TRIP_ID);
      given(tripAccessValidator.validateAccessibleTrip(TRIP_ID, owner)).willReturn(trip);
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
      ShareCreateResponseDTO result = shareService.createShare(request, owner);

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
      ShareCreateRequestDTO selfRequest = new ShareCreateRequestDTO(TEST_TRIP_KEY, OWNER_ID);

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
}