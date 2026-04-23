package com.triptyche.backend.domain.trip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.triptyche.backend.domain.trip.model.PinPoint;
import com.triptyche.backend.domain.trip.model.Trip;
import com.triptyche.backend.domain.trip.model.TripStatus;
import com.triptyche.backend.domain.trip.repository.PinPointRepository;
import com.triptyche.backend.domain.user.model.User;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 테스트 기준 좌표: 서울 시청 (37.5666, 126.9784)
 *
 * 거리 계산 기준 (Haversine):
 * - 200m 이내: 위도 기준 약 0.0018도 차이 (~200m)
 * - 200m 초과: 위도 기준 약 0.003도 차이 (~330m)
 *
 * 실측 검증:
 * - (37.5666, 126.9784) → (37.5684, 126.9784): 약 200m (임계값 정확히 경계)
 * - 이내 테스트: (37.5674, 126.9784) → 약 89m
 * - 밖 테스트: (37.5696, 126.9784) → 약 333m
 */
@ExtendWith(MockitoExtension.class)
class PinPointServiceTest {

    // 서울 시청 기준 좌표
    private static final double BASE_LAT = 37.5666;
    private static final double BASE_LON = 126.9784;

    // 200m 이내: 약 89m 거리 (위도 0.0008도 차이)
    private static final double NEAR_LAT = 37.5674;
    private static final double NEAR_LON = 126.9784;

    // 200m 밖: 약 333m 거리 (위도 0.003도 차이)
    private static final double FAR_LAT = 37.5696;
    private static final double FAR_LON = 126.9784;

    @Mock
    private PinPointRepository pinPointRepository;

    @InjectMocks
    private PinPointService pinPointService;

    private Trip trip;

    @BeforeEach
    void setUp() {
        User owner = User.builder()
                .userId(1L)
                .userName("소유자")
                .userNickName("ownerNick")
                .userEmail("owner@example.com")
                .provider("google")
                .build();

        trip = Trip.builder()
                .tripId(1L)
                .tripTitle("서울 여행")
                .country("한국")
                .startDate(LocalDate.of(2024, 5, 1))
                .endDate(LocalDate.of(2024, 5, 7))
                .hashtags("서울,여행")
                .status(TripStatus.CONFIRMED)
                .user(owner)
                .build();
    }

    private PinPoint createPinPoint(Long id, Double latitude, Double longitude) {
        return PinPoint.builder()
                .pinPointId(id)
                .trip(trip)
                .latitude(latitude)
                .longitude(longitude)
                .build();
    }

    @Nested
    @DisplayName("assignPinPointWithQuery()")
    class FindOrCreatePinPoint {

        @Test
        @DisplayName("200m 이내에 기존 PinPoint가 있으면 새로 저장하지 않고 기존 PinPoint를 반환한다")
        void assignPinPointWithQuery_givenNearbyPinPoint_returnsExistingWithoutSave() {
            // given
            PinPoint existingPinPoint = createPinPoint(10L, BASE_LAT, BASE_LON);
            given(pinPointRepository.findByTripTripId(1L)).willReturn(List.of(existingPinPoint));

            // when
            // NEAR_LAT는 BASE_LAT로부터 약 89m 거리 — 200m 임계값 이내
            PinPoint result = pinPointService.assignPinPointWithQuery(trip, NEAR_LAT, NEAR_LON);

            // then
            assertThat(result).isEqualTo(existingPinPoint);
            verify(pinPointRepository, never()).save(any(PinPoint.class));
        }

        @Test
        @DisplayName("200m 이내에 기존 PinPoint가 있으면 반환된 PinPoint의 ID가 기존 PinPoint의 ID와 일치한다")
        void assignPinPointWithQuery_givenNearbyPinPoint_returnedPinPointHasCorrectId() {
            // given
            PinPoint existingPinPoint = createPinPoint(10L, BASE_LAT, BASE_LON);
            given(pinPointRepository.findByTripTripId(1L)).willReturn(List.of(existingPinPoint));

            // when
            PinPoint result = pinPointService.assignPinPointWithQuery(trip, NEAR_LAT, NEAR_LON);

            // then
            assertThat(result.getPinPointId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("200m 초과 거리에 기존 PinPoint만 있으면 새 PinPoint를 생성하고 저장한다")
        void assignPinPointWithQuery_givenOnlyFarPinPoints_savesAndReturnsNewPinPoint() {
            // given
            PinPoint farPinPoint = createPinPoint(10L, BASE_LAT, BASE_LON);
            given(pinPointRepository.findByTripTripId(1L)).willReturn(List.of(farPinPoint));

            PinPoint savedPinPoint = createPinPoint(20L, FAR_LAT, FAR_LON);
            given(pinPointRepository.save(any(PinPoint.class))).willReturn(savedPinPoint);

            // when
            // FAR_LAT는 BASE_LAT로부터 약 333m 거리 — 200m 임계값 초과
            PinPoint result = pinPointService.assignPinPointWithQuery(trip, FAR_LAT, FAR_LON);

            // then
            verify(pinPointRepository).save(any(PinPoint.class));
            assertThat(result.getPinPointId()).isEqualTo(20L);
        }

        @Test
        @DisplayName("기존 PinPoint가 하나도 없으면 새 PinPoint를 생성하고 저장한다")
        void assignPinPointWithQuery_givenNoPinPoints_savesAndReturnsNewPinPoint() {
            // given
            given(pinPointRepository.findByTripTripId(1L)).willReturn(List.of());

            PinPoint savedPinPoint = createPinPoint(1L, BASE_LAT, BASE_LON);
            given(pinPointRepository.save(any(PinPoint.class))).willReturn(savedPinPoint);

            // when
            PinPoint result = pinPointService.assignPinPointWithQuery(trip, BASE_LAT, BASE_LON);

            // then
            verify(pinPointRepository).save(any(PinPoint.class));
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("여러 PinPoint 중 200m 이내인 첫 번째 PinPoint를 반환한다")
        void assignPinPointWithQuery_givenMultiplePinPointsWithOneNearby_returnsNearestFirst() {
            // given
            PinPoint farPinPoint = createPinPoint(10L, FAR_LAT, FAR_LON);
            PinPoint nearPinPoint = createPinPoint(20L, BASE_LAT, BASE_LON);
            // farPinPoint가 리스트 앞에 오도록 설정 — 200m 초과이므로 건너뛰고 nearPinPoint 반환
            given(pinPointRepository.findByTripTripId(1L)).willReturn(List.of(farPinPoint, nearPinPoint));

            // when
            // NEAR_LAT는 BASE_LAT로부터 약 89m, FAR_LAT로부터는 약 244m
            PinPoint result = pinPointService.assignPinPointWithQuery(trip, NEAR_LAT, NEAR_LON);

            // then
            assertThat(result.getPinPointId()).isEqualTo(20L);
            verify(pinPointRepository, never()).save(any(PinPoint.class));
        }

        @Test
        @DisplayName("새 PinPoint 생성 시 요청한 여행과 좌표로 PinPoint가 저장된다")
        void assignPinPointWithQuery_givenNoPinPoints_savedPinPointHasCorrectTripAndCoordinates() {
            // given
            given(pinPointRepository.findByTripTripId(1L)).willReturn(List.of());
            given(pinPointRepository.save(any(PinPoint.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            PinPoint result = pinPointService.assignPinPointWithQuery(trip, BASE_LAT, BASE_LON);

            // then
            assertThat(result.getLatitude()).isEqualTo(BASE_LAT);
            assertThat(result.getLongitude()).isEqualTo(BASE_LON);
            assertThat(result.getTrip()).isEqualTo(trip);
        }
    }

    @Nested
    @DisplayName("assignPinPoint()")
    class FindOrCreateFromList {

        @Test
        @DisplayName("200m 이내에 기존 PinPoint가 있으면 기존 PinPoint를 반환하고 목록 크기가 변하지 않는다")
        void assignPinPoint_givenNearbyPinPoint_returnsExistingAndListSizeUnchanged() {
            // given
            PinPoint existingPinPoint = createPinPoint(10L, BASE_LAT, BASE_LON);
            List<PinPoint> existingPinPoints = new ArrayList<>(List.of(existingPinPoint));
            int originalSize = existingPinPoints.size();

            // when
            PinPoint result = pinPointService.assignPinPoint(existingPinPoints, trip, NEAR_LAT, NEAR_LON);

            // then
            assertThat(result).isEqualTo(existingPinPoint);
            assertThat(existingPinPoints).hasSize(originalSize);
        }

        @Test
        @DisplayName("200m 이내에 기존 PinPoint가 있으면 repository.save()를 호출하지 않는다")
        void assignPinPoint_givenNearbyPinPoint_doesNotCallSave() {
            // given
            PinPoint existingPinPoint = createPinPoint(10L, BASE_LAT, BASE_LON);
            List<PinPoint> existingPinPoints = new ArrayList<>(List.of(existingPinPoint));

            // when
            pinPointService.assignPinPoint(existingPinPoints, trip, NEAR_LAT, NEAR_LON);

            // then
            verify(pinPointRepository, never()).save(any(PinPoint.class));
        }

        @Test
        @DisplayName("200m 초과 거리에만 기존 PinPoint가 있으면 새 PinPoint를 생성하고 목록에 추가한다")
        void assignPinPoint_givenOnlyFarPinPoints_savesNewAndAddsToList() {
            // given
            PinPoint farPinPoint = createPinPoint(10L, BASE_LAT, BASE_LON);
            List<PinPoint> existingPinPoints = new ArrayList<>(List.of(farPinPoint));

            PinPoint savedPinPoint = createPinPoint(20L, FAR_LAT, FAR_LON);
            given(pinPointRepository.save(any(PinPoint.class))).willReturn(savedPinPoint);

            // when
            PinPoint result = pinPointService.assignPinPoint(existingPinPoints, trip, FAR_LAT, FAR_LON);

            // then
            assertThat(result.getPinPointId()).isEqualTo(20L);
            assertThat(existingPinPoints).hasSize(2);
            assertThat(existingPinPoints).contains(savedPinPoint);
        }

        @Test
        @DisplayName("빈 목록으로 호출하면 새 PinPoint를 생성하고 목록에 추가한다")
        void assignPinPoint_givenEmptyList_savesNewAndAddsToList() {
            // given
            List<PinPoint> existingPinPoints = new ArrayList<>();
            PinPoint savedPinPoint = createPinPoint(1L, BASE_LAT, BASE_LON);
            given(pinPointRepository.save(any(PinPoint.class))).willReturn(savedPinPoint);

            // when
            PinPoint result = pinPointService.assignPinPoint(existingPinPoints, trip, BASE_LAT, BASE_LON);

            // then
            verify(pinPointRepository).save(any(PinPoint.class));
            assertThat(existingPinPoints).hasSize(1);
            assertThat(result).isEqualTo(savedPinPoint);
        }

        @Test
        @DisplayName("새로 생성된 PinPoint는 existingPinPoints 목록의 마지막 원소로 추가된다")
        void assignPinPoint_givenEmptyList_newPinPointIsLastElementInList() {
            // given
            PinPoint firstPinPoint = createPinPoint(1L, FAR_LAT, FAR_LON);
            List<PinPoint> existingPinPoints = new ArrayList<>(List.of(firstPinPoint));

            // FAR_LAT 기준 좌표에서 다시 FAR_LAT를 요청하면 기존 것이 반환되지만,
            // 완전히 다른 좌표(BASE_LAT)로 요청하면 새로 생성된다
            // 여기서는 existingPinPoints[0]이 FAR_LAT이고, 요청 좌표는 BASE_LAT → 거리 약 333m → 새로 생성
            PinPoint savedPinPoint = createPinPoint(99L, BASE_LAT, BASE_LON);
            given(pinPointRepository.save(any(PinPoint.class))).willReturn(savedPinPoint);

            // when
            pinPointService.assignPinPoint(existingPinPoints, trip, BASE_LAT, BASE_LON);

            // then
            assertThat(existingPinPoints).hasSize(2);
            assertThat(existingPinPoints.get(existingPinPoints.size() - 1)).isEqualTo(savedPinPoint);
        }

        @Test
        @DisplayName("새 PinPoint 생성 시 요청한 여행과 좌표로 PinPoint가 저장된다")
        void assignPinPoint_givenEmptyList_savedPinPointHasCorrectTripAndCoordinates() {
            // given
            List<PinPoint> existingPinPoints = new ArrayList<>();
            given(pinPointRepository.save(any(PinPoint.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            PinPoint result = pinPointService.assignPinPoint(existingPinPoints, trip, BASE_LAT, BASE_LON);

            // then
            assertThat(result.getLatitude()).isEqualTo(BASE_LAT);
            assertThat(result.getLongitude()).isEqualTo(BASE_LON);
            assertThat(result.getTrip()).isEqualTo(trip);
        }

        @Test
        @DisplayName("여러 번 호출 시 매 호출마다 목록이 누적되어 배치 처리가 가능하다")
        void assignPinPoint_givenMultipleCalls_listAccumulatesAcrossCalls() {
            // given
            List<PinPoint> existingPinPoints = new ArrayList<>();

            PinPoint firstSaved = createPinPoint(1L, BASE_LAT, BASE_LON);
            PinPoint secondSaved = createPinPoint(2L, FAR_LAT, FAR_LON);
            given(pinPointRepository.save(any(PinPoint.class)))
                    .willReturn(firstSaved)
                    .willReturn(secondSaved);

            // when
            // 첫 번째 호출: 빈 목록 → 신규 생성
            PinPoint first = pinPointService.assignPinPoint(existingPinPoints, trip, BASE_LAT, BASE_LON);
            // 두 번째 호출: FAR_LAT 요청, 목록에는 BASE_LAT PinPoint만 존재 (거리 약 333m) → 신규 생성
            PinPoint second = pinPointService.assignPinPoint(existingPinPoints, trip, FAR_LAT, FAR_LON);

            // then
            assertThat(existingPinPoints).hasSize(2);
            assertThat(first.getPinPointId()).isEqualTo(1L);
            assertThat(second.getPinPointId()).isEqualTo(2L);
        }
    }
}