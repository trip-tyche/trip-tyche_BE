package com.triptyche.backend.domain.media.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.triptyche.backend.domain.media.dto.MediaProcessingRequest;
import com.triptyche.backend.domain.media.dto.MediaProcessingResponse;
import com.triptyche.backend.domain.media.event.MediaFileRegisteredEvent;
import com.triptyche.backend.domain.media.model.MediaFile;
import com.triptyche.backend.domain.media.model.ProcessingStatus;
import com.triptyche.backend.domain.media.repository.MediaFileRepository;
import com.triptyche.backend.domain.trip.model.PinPoint;
import com.triptyche.backend.domain.trip.model.Trip;
import com.triptyche.backend.domain.trip.service.PinPointService;
import com.triptyche.backend.domain.trip.service.TripAccessGuard;
import com.triptyche.backend.domain.user.model.User;
import com.triptyche.backend.global.common.ResultCode;
import com.triptyche.backend.global.exception.CustomException;
import com.triptyche.backend.global.s3.S3KeyResolver;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
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
class MediaCommandServiceTest {

    @Mock
    private MediaFileRepository mediaFileRepository;
    @Mock
    private PinPointService pinPointService;
    @Mock
    private S3KeyResolver s3KeyResolver;
    @Mock
    private TripAccessGuard tripAccessGuard;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private MediaCommandService mediaCommandService;

    private static final String TRIP_KEY = "TRIP-KEY-001";
    private static final Long TRIP_ID = 42L;
    private static final Long USER_ID = 10L;
    private static final Long OTHER_TRIP_ID = 99L;

    private User user;
    private Trip trip;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .userId(USER_ID)
                .userName("테스터")
                .userNickName("tester")
                .userEmail("tester@example.com")
                .provider("google")
                .build();

        trip = Trip.builder()
                .tripId(TRIP_ID)
                .tripTitle("테스트 여행")
                .country("일본")
                .user(user)
                .build();
    }

    @Nested
    @DisplayName("requestProcessing()")
    class MarkUploadedAndEnqueue {

        @Test
        @DisplayName("happy path: 2개 row → 응답 2건, status=UPLOADED, currentUrl/finalUrl 올바름")
        void happyPath_twoItems_returnsCorrectResponse() {
            // given
            given(tripAccessGuard.validateAccessibleTripByKey(TRIP_KEY, user)).willReturn(trip);

            PinPoint pinPoint = PinPoint.builder()
                    .pinPointId(1L)
                    .trip(trip)
                    .latitude(37.5)
                    .longitude(127.0)
                    .build();

            MediaFile mf1 = MediaFile.builder()
                    .mediaFileId(101L)
                    .trip(trip)
                    .mediaType("image/webp")
                    .mediaKey("processed/42/uuid1.webp")
                    .tempKey("temp/42/uuid1.jpg")
                    .finalKey("processed/42/uuid1.webp")
                    .build();

            MediaFile mf2 = MediaFile.builder()
                    .mediaFileId(102L)
                    .trip(trip)
                    .mediaType("image/webp")
                    .mediaKey("processed/42/uuid2.webp")
                    .tempKey("temp/42/uuid2.jpg")
                    .finalKey("processed/42/uuid2.webp")
                    .build();

            given(mediaFileRepository.findAllById(List.of(101L, 102L)))
                    .willReturn(List.of(mf1, mf2));
            given(pinPointService.findAllByTripId(TRIP_ID)).willReturn(List.of(pinPoint));
            given(pinPointService.assignPinPoint(any(), any(), any(), any())).willReturn(pinPoint);
            given(s3KeyResolver.buildUrl("temp/42/uuid1.jpg")).willReturn("https://s3.example.com/temp/42/uuid1.jpg");
            given(s3KeyResolver.buildUrl("processed/42/uuid1.webp")).willReturn("https://s3.example.com/processed/42/uuid1.webp");
            given(s3KeyResolver.buildUrl("temp/42/uuid2.jpg")).willReturn("https://s3.example.com/temp/42/uuid2.jpg");
            given(s3KeyResolver.buildUrl("processed/42/uuid2.webp")).willReturn("https://s3.example.com/processed/42/uuid2.webp");

            MediaProcessingRequest request = new MediaProcessingRequest(List.of(
                    new MediaProcessingRequest.Item(101L, "2024-01-01T12:00:00", 37.5, 127.0),
                    new MediaProcessingRequest.Item(102L, "2024-01-01T13:00:00", 37.5, 127.0)
            ));

            // when
            MediaProcessingResponse response = mediaCommandService.requestProcessing(user, TRIP_KEY, request);

            // then
            assertThat(response.items()).hasSize(2);

            MediaProcessingResponse.Item item1 = response.items().get(0);
            assertThat(item1.mediaFileId()).isEqualTo(101L);
            assertThat(item1.currentUrl()).isEqualTo("https://s3.example.com/temp/42/uuid1.jpg");
            assertThat(item1.finalUrl()).isEqualTo("https://s3.example.com/processed/42/uuid1.webp");
            assertThat(item1.status()).isEqualTo(ProcessingStatus.UPLOADED.name());

            MediaProcessingResponse.Item item2 = response.items().get(1);
            assertThat(item2.mediaFileId()).isEqualTo(102L);
            assertThat(item2.status()).isEqualTo(ProcessingStatus.UPLOADED.name());
        }

        @Test
        @DisplayName("미존재 mediaFileId → MEDIA_FILE_NOT_FOUND 예외")
        void unknownMediaFileId_throwsMediaFileNotFound() {
            // given
            given(tripAccessGuard.validateAccessibleTripByKey(TRIP_KEY, user)).willReturn(trip);
            // findAllById returns only 1 of 2 requested
            MediaFile mf1 = MediaFile.builder()
                    .mediaFileId(101L)
                    .trip(trip)
                    .tempKey("temp/42/uuid1.jpg")
                    .finalKey("processed/42/uuid1.webp")
                    .mediaKey("processed/42/uuid1.webp")
                    .build();
            given(mediaFileRepository.findAllById(List.of(101L, 999L)))
                    .willReturn(List.of(mf1));

            MediaProcessingRequest request = new MediaProcessingRequest(List.of(
                    new MediaProcessingRequest.Item(101L, "2024-01-01T12:00:00", 37.5, 127.0),
                    new MediaProcessingRequest.Item(999L, "2024-01-01T13:00:00", 37.5, 127.0)
            ));

            // when / then
            assertThatThrownBy(() -> mediaCommandService.requestProcessing(user, TRIP_KEY, request))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getResultCode())
                    .isEqualTo(ResultCode.MEDIA_FILE_NOT_FOUND);
        }

        @Test
        @DisplayName("다른 trip의 row → ACCESS_DENIED 예외")
        void mediaFileBelongsToDifferentTrip_throwsAccessDenied() {
            // given
            given(tripAccessGuard.validateAccessibleTripByKey(TRIP_KEY, user)).willReturn(trip);

            Trip otherTrip = Trip.builder()
                    .tripId(OTHER_TRIP_ID)
                    .tripTitle("남의 여행")
                    .country("프랑스")
                    .user(user)
                    .build();

            MediaFile mf = MediaFile.builder()
                    .mediaFileId(101L)
                    .trip(otherTrip)
                    .tempKey("temp/99/uuid1.jpg")
                    .finalKey("processed/99/uuid1.webp")
                    .mediaKey("processed/99/uuid1.webp")
                    .build();

            given(mediaFileRepository.findAllById(List.of(101L))).willReturn(List.of(mf));

            MediaProcessingRequest request = new MediaProcessingRequest(List.of(
                    new MediaProcessingRequest.Item(101L, "2024-01-01T12:00:00", 37.5, 127.0)
            ));

            // when / then
            assertThatThrownBy(() -> mediaCommandService.requestProcessing(user, TRIP_KEY, request))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getResultCode())
                    .isEqualTo(ResultCode.ACCESS_DENIED);
        }

        @Test
        @DisplayName("v2 이벤트 발행 검증: flow=v2, tempKey/finalKey 포함")
        void publishesV2Event() {
            // given
            given(tripAccessGuard.validateAccessibleTripByKey(TRIP_KEY, user)).willReturn(trip);

            PinPoint pinPoint = PinPoint.builder()
                    .pinPointId(1L)
                    .trip(trip)
                    .latitude(37.5)
                    .longitude(127.0)
                    .build();

            MediaFile mf = MediaFile.builder()
                    .mediaFileId(101L)
                    .trip(trip)
                    .mediaType("image/webp")
                    .mediaKey("processed/42/uuid1.webp")
                    .tempKey("temp/42/uuid1.jpg")
                    .finalKey("processed/42/uuid1.webp")
                    .build();

            given(mediaFileRepository.findAllById(List.of(101L))).willReturn(List.of(mf));
            given(pinPointService.findAllByTripId(TRIP_ID)).willReturn(List.of(pinPoint));
            given(pinPointService.assignPinPoint(any(), any(), any(), any())).willReturn(pinPoint);
            given(s3KeyResolver.buildUrl(any())).willReturn("https://s3.example.com/any");

            MediaProcessingRequest request = new MediaProcessingRequest(List.of(
                    new MediaProcessingRequest.Item(101L, "2024-01-01T12:00:00", 37.5, 127.0)
            ));

            // when
            mediaCommandService.requestProcessing(user, TRIP_KEY, request);

            // then
            ArgumentCaptor<MediaFileRegisteredEvent> captor =
                    ArgumentCaptor.forClass(MediaFileRegisteredEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());

            MediaFileRegisteredEvent published = captor.getValue();
            assertThat(published.flow()).isEqualTo("v2");
            assertThat(published.tempKey()).isEqualTo("temp/42/uuid1.jpg");
            assertThat(published.finalKey()).isEqualTo("processed/42/uuid1.webp");
            assertThat(published.originalKey()).isNull();
            assertThat(published.mediaFileId()).isEqualTo(101L);
        }
    }
}
