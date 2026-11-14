package com.triptyche.backend.domain.trip.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.triptyche.backend.domain.media.dto.TripCoverInfo;
import com.triptyche.backend.domain.media.service.MediaQueryService;
import com.triptyche.backend.domain.share.service.ShareQueryService;
import com.triptyche.backend.domain.trip.dto.TripDetailResponse;
import com.triptyche.backend.domain.trip.dto.TripSummaryResponse;
import com.triptyche.backend.domain.trip.model.Trip;
import com.triptyche.backend.domain.trip.model.TripStatus;
import com.triptyche.backend.domain.trip.repository.PinPointRepository;
import com.triptyche.backend.domain.trip.repository.TripRepository;
import com.triptyche.backend.domain.trip.service.TripAccessGuard;
import com.triptyche.backend.domain.user.model.User;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TripFacadeTest {

  @Mock
  private TripRepository tripRepository;
  @Mock
  private PinPointRepository pinPointRepository;
  @Mock
  private TripAccessGuard tripAccessGuard;
  @Mock
  private MediaQueryService mediaQueryService;
  @Mock
  private ShareQueryService shareQueryService;

  @InjectMocks
  private TripFacade tripFacade;

  private User user;
  private Trip noPhotoTrip;
  private Trip processingOnlyTrip;
  private Trip processedTrip;

  @BeforeEach
  void setUp() {
    user = User.builder()
        .userId(1L)
        .userName("테스터")
        .userNickName("tester")
        .userEmail("tester@example.com")
        .provider("google")
        .build();
    noPhotoTrip = trip(10L, "KEY-10");
    processingOnlyTrip = trip(20L, "KEY-20");
    processedTrip = trip(30L, "KEY-30");
  }

  private Trip trip(Long tripId, String tripKey) {
    return Trip.builder()
        .tripId(tripId)
        .tripKey(tripKey)
        .user(user)
        .tripTitle("여행 " + tripId)
        .country("JAPAN")
        .startDate(LocalDate.of(2025, 1, 1))
        .endDate(LocalDate.of(2025, 1, 5))
        .hashtags("")
        .status(TripStatus.CONFIRMED)
        .build();
  }

  private void givenCovers() {
    given(tripRepository.findAllAccessibleTripsWithOwner(user.getUserId()))
        .willReturn(List.of(noPhotoTrip, processingOnlyTrip, processedTrip));
    // 사진 없는 여행은 Map에 키가 없다. 처리 중 사진만 있는 여행은 cover=null, count>0.
    given(mediaQueryService.findCoverInfosByTripIds(List.of(10L, 20L, 30L)))
        .willReturn(Map.of(
            20L, new TripCoverInfo(null, 3L),
            30L, new TripCoverInfo("https://cdn/processed/30/a.webp", 5L)
        ));
  }

  @Nested
  @DisplayName("getTripsSummary()")
  class GetTripsSummary {

    @Test
    @DisplayName("여행마다 표지·사진 수를 붙이고 조회는 여행 수와 관계없이 1회")
    void attachesCoverAndCount() {
      givenCovers();

      List<TripSummaryResponse> result = tripFacade.getTripsSummary(user).trips();

      assertThat(result).extracting(TripSummaryResponse::tripKey, TripSummaryResponse::coverMediaLink,
              TripSummaryResponse::photoCount)
          .containsExactly(
              tuple("KEY-10", null, 0L),
              tuple("KEY-20", null, 3L),
              tuple("KEY-30", "https://cdn/processed/30/a.webp", 5L));
      verify(mediaQueryService, times(1)).findCoverInfosByTripIds(anyCollection());
    }

    @Test
    @DisplayName("여행이 없으면 표지 조회를 하지 않는다")
    void noTrips() {
      given(tripRepository.findAllAccessibleTripsWithOwner(user.getUserId())).willReturn(List.of());

      assertThat(tripFacade.getTripsSummary(user).trips()).isEmpty();
      verify(mediaQueryService, never()).findCoverInfosByTripIds(any());
    }
  }

  @Nested
  @DisplayName("getTripsByUser()")
  class GetTripsByUser {

    @Test
    @DisplayName("여행마다 표지·사진 수를 붙이고 조회는 여행 수와 관계없이 1회")
    void attachesCoverAndCount() {
      givenCovers();
      given(shareQueryService.findApprovedShareSummariesByTripIds(List.of(10L, 20L, 30L)))
          .willReturn(List.of());

      List<TripDetailResponse> result = tripFacade.getTripsByUser(user).trips();

      assertThat(result).extracting(TripDetailResponse::tripKey, TripDetailResponse::coverMediaLink,
              TripDetailResponse::photoCount)
          .containsExactly(
              tuple("KEY-10", null, 0L),
              tuple("KEY-20", null, 3L),
              tuple("KEY-30", "https://cdn/processed/30/a.webp", 5L));
      verify(mediaQueryService, times(1)).findCoverInfosByTripIds(anyCollection());
    }
  }
}
