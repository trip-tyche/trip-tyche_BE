package com.triptyche.backend.domain.media.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.triptyche.backend.domain.media.dto.PresignedUrlCreateRequest;
import com.triptyche.backend.domain.media.dto.PresignedUrlCreateRequest.FileInfo;
import com.triptyche.backend.domain.media.dto.PresignedUrlResponse;
import com.triptyche.backend.domain.media.dto.PresignedUrlResponse.PresignedUrl;
import com.triptyche.backend.domain.media.model.MediaFile;
import com.triptyche.backend.domain.media.repository.MediaFileRepository;
import com.triptyche.backend.domain.trip.model.Trip;
import com.triptyche.backend.domain.trip.service.TripAccessGuard;
import com.triptyche.backend.domain.user.model.User;
import com.triptyche.backend.global.s3.PresignedURLService;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MediaPresignServiceTest {

    @Mock
    private TripAccessGuard tripAccessGuard;

    @Mock
    private PresignedURLService presignedURLService;

    @Mock
    private MediaFileRepository mediaFileRepository;

    @InjectMocks
    private MediaPresignService mediaPresignService;

    private static final String TRIP_KEY = "TRIP-KEY-001";
    private static final Long TRIP_ID = 42L;

    private User user;
    private Trip trip;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .userId(10L)
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
    @DisplayName("issuePutUrls()")
    class IssuePutUrls {

        @Test
        @DisplayName("N개 파일에 대해 tempKey/finalKey 다른 UUID로 생성, 응답 N건 반환")
        void issuePutUrls_multipleFiles_returnsNResults() {
            // given
            given(tripAccessGuard.validateAccessibleTripByKey(TRIP_KEY, user)).willReturn(trip);

            AtomicLong idSeq = new AtomicLong(100L);
            given(mediaFileRepository.save(any(MediaFile.class))).willAnswer(inv -> {
                MediaFile mf = inv.getArgument(0);
                // simulate JPA id assignment via reflection is not needed — we use a stub
                MediaFile saved = MediaFile.builder()
                        .trip(mf.getTrip())
                        .mediaType(mf.getMediaType())
                        .mediaKey(mf.getMediaKey())
                        .tempKey(mf.getTempKey())
                        .finalKey(mf.getFinalKey())
                        .mediaFileId(idSeq.getAndIncrement())
                        .build();
                return saved;
            });
            given(presignedURLService.generatePresignedPutUrl(any(), any(Duration.class)))
                    .willAnswer(inv -> "https://s3.example.com/put?key=" + inv.getArgument(0));

            PresignedUrlCreateRequest request = new PresignedUrlCreateRequest(List.of(
                    new FileInfo("photo1.jpg"),
                    new FileInfo("photo2.PNG")
            ));

            // when
            PresignedUrlResponse response = mediaPresignService.issuePutUrls(user, TRIP_KEY, request);

            // then
            assertThat(response.presignedUrls()).hasSize(2);

            PresignedUrl first = response.presignedUrls().get(0);
            PresignedUrl second = response.presignedUrls().get(1);

            // tempKey and finalKey are different for each file
            assertThat(first.tempKey()).isNotEqualTo(second.tempKey());
            assertThat(first.finalKey()).isNotEqualTo(second.finalKey());

            // fileKey == tempKey (backward compat)
            assertThat(first.fileKey()).isEqualTo(first.tempKey());
            assertThat(second.fileKey()).isEqualTo(second.tempKey());

            // tempKey starts with temp/, finalKey starts with processed/
            assertThat(first.tempKey()).startsWith("temp/");
            assertThat(first.finalKey()).startsWith("processed/");
            assertThat(first.finalKey()).endsWith(".webp");

            // presignedPutUrl uses tempKey
            assertThat(first.presignedPutUrl()).contains(first.tempKey());

            // mediaFileId assigned
            assertThat(first.mediaFileId()).isNotNull();
            assertThat(second.mediaFileId()).isNotNull();
            assertThat(first.mediaFileId()).isNotEqualTo(second.mediaFileId());
        }

        @Test
        @DisplayName("mediaFileRepository.save()가 N회 호출되고 저장된 MediaFile의 필드가 올바르다")
        void issuePutUrls_savesMediaFileWithCorrectFields() {
            // given
            given(tripAccessGuard.validateAccessibleTripByKey(TRIP_KEY, user)).willReturn(trip);

            AtomicLong idSeq = new AtomicLong(1L);
            given(mediaFileRepository.save(any(MediaFile.class))).willAnswer(inv -> {
                MediaFile mf = inv.getArgument(0);
                return MediaFile.builder()
                        .mediaFileId(idSeq.getAndIncrement())
                        .trip(mf.getTrip())
                        .mediaType(mf.getMediaType())
                        .mediaKey(mf.getMediaKey())
                        .tempKey(mf.getTempKey())
                        .finalKey(mf.getFinalKey())
                        .build();
            });
            given(presignedURLService.generatePresignedPutUrl(any(), any(Duration.class)))
                    .willReturn("https://s3.example.com/put");

            PresignedUrlCreateRequest request = new PresignedUrlCreateRequest(List.of(
                    new FileInfo("img.jpg"),
                    new FileInfo("img2.jpg"),
                    new FileInfo("img3.jpg")
            ));

            // when
            mediaPresignService.issuePutUrls(user, TRIP_KEY, request);

            // then: save called 3 times
            ArgumentCaptor<MediaFile> captor = ArgumentCaptor.forClass(MediaFile.class);
            verify(mediaFileRepository, times(3)).save(captor.capture());

            List<MediaFile> saved = captor.getAllValues();
            for (MediaFile mf : saved) {
                assertThat(mf.getTrip()).isEqualTo(trip);
                assertThat(mf.getMediaType()).isEqualTo("image/webp");
                assertThat(mf.getTempKey()).startsWith("temp/" + TRIP_ID + "/");
                assertThat(mf.getFinalKey()).startsWith("processed/" + TRIP_ID + "/");
                assertThat(mf.getFinalKey()).endsWith(".webp");
                // mediaKey == finalKey
                assertThat(mf.getMediaKey()).isEqualTo(mf.getFinalKey());
                // pinPoint is null (not set at presign time)
                assertThat(mf.getPinPoint()).isNull();
            }
        }

        @Test
        @DisplayName("presignedURLService.generatePresignedPutUrl이 tempKey로 호출된다")
        void issuePutUrls_callsPresignedUrlServiceWithTempKey() {
            // given
            given(tripAccessGuard.validateAccessibleTripByKey(TRIP_KEY, user)).willReturn(trip);

            AtomicLong idSeq = new AtomicLong(1L);
            given(mediaFileRepository.save(any(MediaFile.class))).willAnswer(inv -> {
                MediaFile mf = inv.getArgument(0);
                return MediaFile.builder()
                        .mediaFileId(idSeq.getAndIncrement())
                        .trip(mf.getTrip())
                        .mediaType(mf.getMediaType())
                        .mediaKey(mf.getMediaKey())
                        .tempKey(mf.getTempKey())
                        .finalKey(mf.getFinalKey())
                        .build();
            });
            given(presignedURLService.generatePresignedPutUrl(any(), any(Duration.class)))
                    .willReturn("https://s3.example.com/put");

            PresignedUrlCreateRequest request = new PresignedUrlCreateRequest(List.of(
                    new FileInfo("photo.jpg")
            ));

            // when
            PresignedUrlResponse response = mediaPresignService.issuePutUrls(user, TRIP_KEY, request);

            // then
            String usedTempKey = response.presignedUrls().get(0).tempKey();
            verify(presignedURLService).generatePresignedPutUrl(eq(usedTempKey), any(Duration.class));
        }
    }

    @Nested
    @DisplayName("extractExtension() — private static method via issuePutUrls 통합 검증")
    class ExtractExtension {

        @BeforeEach
        void stubCommon() {
            given(tripAccessGuard.validateAccessibleTripByKey(TRIP_KEY, user)).willReturn(trip);
            given(mediaFileRepository.save(any(MediaFile.class))).willAnswer(inv -> {
                MediaFile mf = inv.getArgument(0);
                return MediaFile.builder()
                        .mediaFileId(1L)
                        .trip(mf.getTrip())
                        .mediaType(mf.getMediaType())
                        .mediaKey(mf.getMediaKey())
                        .tempKey(mf.getTempKey())
                        .finalKey(mf.getFinalKey())
                        .build();
            });
            given(presignedURLService.generatePresignedPutUrl(any(), any(Duration.class)))
                    .willReturn("https://s3.example.com/put");
        }

        @Test
        @DisplayName("foo.JPG → tempKey에 .jpg 포함")
        void extractExtension_uppercaseJpg_normalized() {
            PresignedUrlCreateRequest request = new PresignedUrlCreateRequest(List.of(new FileInfo("foo.JPG")));

            PresignedUrlResponse response = mediaPresignService.issuePutUrls(user, TRIP_KEY, request);

            assertThat(response.presignedUrls().get(0).tempKey()).endsWith(".jpg");
        }

        @Test
        @DisplayName("확장자 없는 파일명 → tempKey에 .jpg(기본값) 포함")
        void extractExtension_noExtension_defaultsToJpg() {
            PresignedUrlCreateRequest request = new PresignedUrlCreateRequest(List.of(new FileInfo("foo")));

            PresignedUrlResponse response = mediaPresignService.issuePutUrls(user, TRIP_KEY, request);

            assertThat(response.presignedUrls().get(0).tempKey()).endsWith(".jpg");
        }

        @Test
        @DisplayName("파일명이 '.'으로 끝나는 경우 → tempKey에 .jpg(기본값) 포함")
        void extractExtension_trailingDot_defaultsToJpg() {
            PresignedUrlCreateRequest request = new PresignedUrlCreateRequest(List.of(new FileInfo("foo.")));

            PresignedUrlResponse response = mediaPresignService.issuePutUrls(user, TRIP_KEY, request);

            assertThat(response.presignedUrls().get(0).tempKey()).endsWith(".jpg");
        }
    }
}
