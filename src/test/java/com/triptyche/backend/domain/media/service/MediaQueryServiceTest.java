package com.triptyche.backend.domain.media.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.triptyche.backend.domain.media.dto.TripCoverInfo;
import com.triptyche.backend.domain.media.dto.TripCoverRow;
import com.triptyche.backend.domain.media.dto.TripPhotoCountRow;
import com.triptyche.backend.domain.media.dto.UploadStatusResponse;
import com.triptyche.backend.domain.media.model.MediaFile;
import com.triptyche.backend.domain.media.model.ProcessingStatus;
import com.triptyche.backend.domain.media.repository.MediaFileRepository;
import com.triptyche.backend.domain.trip.model.Trip;
import com.triptyche.backend.domain.trip.service.TripAccessGuard;
import com.triptyche.backend.domain.user.model.User;
import com.triptyche.backend.global.s3.S3KeyResolver;
import java.time.LocalDateTime;
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
class MediaQueryServiceTest {

    @Mock
    private MediaFileRepository mediaFileRepository;
    @Mock
    private TripAccessGuard tripAccessGuard;
    @Mock
    private UnlocatedMediaCacheService unlocatedMediaCacheService;
    @Mock
    private S3KeyResolver s3KeyResolver;

    @InjectMocks
    private MediaQueryService mediaQueryService;

    private static final String TRIP_KEY = "TRIP-KEY-001";
    private static final Long TRIP_ID = 42L;
    private static final Long USER_ID = 10L;

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
    @DisplayName("getUploadStatus()")
    class GetUploadStatus {

        @Test
        @DisplayName("다양한 status가 섞인 4건 → 각각 올바른 status 문자열 매핑")
        void mixedStatusRows_returnCorrectStatusStrings() {
            MediaFile uploaded = MediaFile.builder()
                    .mediaFileId(1L)
                    .trip(trip)
                    .mediaKey("key1")
                    .tempKey("temp/42/uuid1.jpg")
                    .processingStatus(ProcessingStatus.UPLOADED)
                    .mediaLink("https://s3/bucket/legacy1")
                    .build();

            MediaFile processing = MediaFile.builder()
                    .mediaFileId(2L)
                    .trip(trip)
                    .mediaKey("key2")
                    .tempKey("temp/42/uuid2.jpg")
                    .processingStatus(ProcessingStatus.PROCESSING)
                    .mediaLink("https://s3/bucket/legacy2")
                    .build();

            MediaFile processed = MediaFile.builder()
                    .mediaFileId(3L)
                    .trip(trip)
                    .mediaKey("key3")
                    .tempKey("temp/42/uuid3.jpg")
                    .finalKey("processed/42/uuid3.webp")
                    .processingStatus(ProcessingStatus.PROCESSED)
                    .processedAt(LocalDateTime.of(2025, 1, 5, 12, 0))
                    .mediaLink("https://s3/bucket/processed3")
                    .build();

            MediaFile failed = MediaFile.builder()
                    .mediaFileId(4L)
                    .trip(trip)
                    .mediaKey("key4")
                    .tempKey("temp/42/uuid4.jpg")
                    .processingStatus(ProcessingStatus.FAILED)
                    .failureReason("변환 실패")
                    .mediaLink("https://s3/bucket/legacy4")
                    .build();

            given(tripAccessGuard.validateAccessibleTripByKey(TRIP_KEY, user)).willReturn(trip);
            given(mediaFileRepository.findByTripTripIdWithPinPoint(TRIP_ID))
                    .willReturn(List.of(uploaded, processing, processed, failed));
            given(s3KeyResolver.buildUrl("temp/42/uuid1.jpg")).willReturn("https://s3/bucket/temp/42/uuid1.jpg");
            given(s3KeyResolver.buildUrl("temp/42/uuid2.jpg")).willReturn("https://s3/bucket/temp/42/uuid2.jpg");
            given(s3KeyResolver.buildUrl("temp/42/uuid3.jpg")).willReturn("https://s3/bucket/temp/42/uuid3.jpg");
            given(s3KeyResolver.buildUrl("processed/42/uuid3.webp")).willReturn("https://s3/bucket/processed/42/uuid3.webp");
            given(s3KeyResolver.buildUrl("temp/42/uuid4.jpg")).willReturn("https://s3/bucket/temp/42/uuid4.jpg");

            UploadStatusResponse response = mediaQueryService.getUploadStatus(user, TRIP_KEY);

            assertThat(response.items()).hasSize(4);
            assertThat(response.items()).extracting(UploadStatusResponse.Item::status)
                    .containsExactly("UPLOADED", "PROCESSING", "PROCESSED", "FAILED");
        }

        @Test
        @DisplayName("processingStatus LEGACY → status='LEGACY', currentUrl/finalUrl은 mediaLink 폴백")
        void legacyProcessingStatus_returnsLegacyStatus() {
            MediaFile legacy = MediaFile.builder()
                    .mediaFileId(10L)
                    .trip(trip)
                    .mediaKey("legacy-key")
                    .processingStatus(ProcessingStatus.LEGACY)
                    .mediaLink("https://s3/bucket/legacy-link")
                    .build();

            given(tripAccessGuard.validateAccessibleTripByKey(TRIP_KEY, user)).willReturn(trip);
            given(mediaFileRepository.findByTripTripIdWithPinPoint(TRIP_ID)).willReturn(List.of(legacy));

            UploadStatusResponse response = mediaQueryService.getUploadStatus(user, TRIP_KEY);

            assertThat(response.items()).hasSize(1);
            UploadStatusResponse.Item item = response.items().get(0);
            assertThat(item.status()).isEqualTo(UploadStatusResponse.LEGACY_STATUS);
            assertThat(item.currentUrl()).isEqualTo("https://s3/bucket/legacy-link");
            assertThat(item.finalUrl()).isEqualTo("https://s3/bucket/legacy-link");
            assertThat(item.failureReason()).isNull();
            assertThat(item.processedAt()).isNull();
        }

        @Test
        @DisplayName("FAILED row → failureReason 포함")
        void failedRow_includesFailureReason() {
            MediaFile failed = MediaFile.builder()
                    .mediaFileId(20L)
                    .trip(trip)
                    .mediaKey("fail-key")
                    .tempKey("temp/42/fail.jpg")
                    .processingStatus(ProcessingStatus.FAILED)
                    .failureReason("변환 오류")
                    .mediaLink("https://s3/bucket/fail-link")
                    .build();

            given(tripAccessGuard.validateAccessibleTripByKey(TRIP_KEY, user)).willReturn(trip);
            given(mediaFileRepository.findByTripTripIdWithPinPoint(TRIP_ID)).willReturn(List.of(failed));
            given(s3KeyResolver.buildUrl("temp/42/fail.jpg")).willReturn("https://s3/bucket/temp/42/fail.jpg");

            UploadStatusResponse response = mediaQueryService.getUploadStatus(user, TRIP_KEY);

            UploadStatusResponse.Item item = response.items().get(0);
            assertThat(item.status()).isEqualTo("FAILED");
            assertThat(item.failureReason()).isEqualTo("변환 오류");
        }

        @Test
        @DisplayName("PROCESSED row → failureReason=null, processedAt 매핑")
        void processedRow_nullFailureReasonAndProcessedAtMapped() {
            LocalDateTime processedAt = LocalDateTime.of(2025, 3, 10, 9, 30);
            MediaFile processed = MediaFile.builder()
                    .mediaFileId(30L)
                    .trip(trip)
                    .mediaKey("proc-key")
                    .tempKey("temp/42/proc.jpg")
                    .finalKey("processed/42/proc.webp")
                    .processingStatus(ProcessingStatus.PROCESSED)
                    .processedAt(processedAt)
                    .mediaLink("https://s3/bucket/proc-link")
                    .build();

            given(tripAccessGuard.validateAccessibleTripByKey(TRIP_KEY, user)).willReturn(trip);
            given(mediaFileRepository.findByTripTripIdWithPinPoint(TRIP_ID)).willReturn(List.of(processed));
            given(s3KeyResolver.buildUrl("temp/42/proc.jpg")).willReturn("https://s3/bucket/temp/42/proc.jpg");
            given(s3KeyResolver.buildUrl("processed/42/proc.webp")).willReturn("https://s3/bucket/processed/42/proc.webp");

            UploadStatusResponse response = mediaQueryService.getUploadStatus(user, TRIP_KEY);

            UploadStatusResponse.Item item = response.items().get(0);
            assertThat(item.status()).isEqualTo("PROCESSED");
            assertThat(item.failureReason()).isNull();
            assertThat(item.processedAt()).isEqualTo(processedAt.toString());
            assertThat(item.finalUrl()).isEqualTo("https://s3/bucket/processed/42/proc.webp");
        }

        @Test
        @DisplayName("tempKey/finalKey 모두 null인 LEGACY row → currentUrl/finalUrl 모두 mediaLink 폴백")
        void noKeys_fallbackToMediaLink() {
            MediaFile noKeys = MediaFile.builder()
                    .mediaFileId(40L)
                    .trip(trip)
                    .mediaKey("no-keys")
                    .processingStatus(ProcessingStatus.LEGACY)
                    .mediaLink("https://s3/bucket/original-link")
                    .build();

            given(tripAccessGuard.validateAccessibleTripByKey(TRIP_KEY, user)).willReturn(trip);
            given(mediaFileRepository.findByTripTripIdWithPinPoint(TRIP_ID)).willReturn(List.of(noKeys));

            UploadStatusResponse response = mediaQueryService.getUploadStatus(user, TRIP_KEY);

            UploadStatusResponse.Item item = response.items().get(0);
            assertThat(item.currentUrl()).isEqualTo("https://s3/bucket/original-link");
            assertThat(item.finalUrl()).isEqualTo("https://s3/bucket/original-link");
        }
    }

    @Nested
    @DisplayName("findCoverInfosByTripIds()")
    class FindCoverInfosByTripIds {

        private TripCoverRow coverRow(Long tripId, String mediaLink) {
            return new TripCoverRow() {
                @Override
                public Long getTripId() {
                    return tripId;
                }

                @Override
                public String getMediaLink() {
                    return mediaLink;
                }
            };
        }

        private TripPhotoCountRow countRow(Long tripId, Long count) {
            return new TripPhotoCountRow() {
                @Override
                public Long getTripId() {
                    return tripId;
                }

                @Override
                public Long getPhotoCount() {
                    return count;
                }
            };
        }

        @Test
        @DisplayName("표지·사진 수를 여행별로 합치고, 처리 중 사진만 있으면 cover=null")
        void mergesCoverAndCount() {
            List<Long> tripIds = List.of(1L, 2L, 3L);
            given(mediaFileRepository.findCoversByTripIds(eq(tripIds), any()))
                    .willReturn(List.of(coverRow(1L, "link-1")));
            given(mediaFileRepository.countByTripIds(tripIds))
                    .willReturn(List.of(countRow(1L, 4L), countRow(2L, 2L)));

            Map<Long, TripCoverInfo> result = mediaQueryService.findCoverInfosByTripIds(tripIds);

            assertThat(result).containsOnly(
                    Map.entry(1L, new TripCoverInfo("link-1", 4L)),
                    Map.entry(2L, new TripCoverInfo(null, 2L)));
            verify(mediaFileRepository, times(1)).findCoversByTripIds(any(), any());
            verify(mediaFileRepository, times(1)).countByTripIds(any());
        }

        @Test
        @DisplayName("1980 기본 날짜를 정렬 기준으로 넘긴다")
        void passesDefaultDate() {
            List<Long> tripIds = List.of(1L);
            given(mediaFileRepository.countByTripIds(tripIds)).willReturn(List.of());

            mediaQueryService.findCoverInfosByTripIds(tripIds);

            verify(mediaFileRepository).findCoversByTripIds(tripIds, LocalDateTime.of(1980, 1, 1, 0, 0, 0));
        }

        @Test
        @DisplayName("빈 목록이면 쿼리를 실행하지 않는다")
        void emptyIds() {
            assertThat(mediaQueryService.findCoverInfosByTripIds(List.of())).isEmpty();
            verify(mediaFileRepository, never()).findCoversByTripIds(any(), any());
            verify(mediaFileRepository, never()).countByTripIds(any());
        }
    }
}
