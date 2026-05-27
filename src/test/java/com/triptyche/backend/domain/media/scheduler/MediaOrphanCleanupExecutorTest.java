package com.triptyche.backend.domain.media.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.triptyche.backend.domain.media.event.MediaFilesS3DeleteRequestedEvent;
import com.triptyche.backend.domain.media.model.MediaFile;
import com.triptyche.backend.domain.media.repository.MediaFileRepository;
import com.triptyche.backend.domain.trip.model.Trip;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class MediaOrphanCleanupExecutorTest {

    @Mock
    private MediaFileRepository mediaFileRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private MediaOrphanCleanupExecutor executor;

    private MediaFile buildOrphan(String tempKey) {
        Trip trip = Trip.builder()
                .user(null)
                .tripTitle("테스트여행")
                .country("KR")
                .startDate(LocalDate.of(2025, 1, 1))
                .endDate(LocalDate.of(2025, 1, 10))
                .hashtags("여행")
                .build();

        return MediaFile.builder()
                .trip(trip)
                .mediaKey("media-key-" + tempKey)
                .tempKey(tempKey)
                .build();
    }

    @Nested
    @DisplayName("orphan이 없는 경우")
    class WhenNoOrphans {

        @Test
        @DisplayName("deleteAll과 이벤트 발행 없이 0을 반환한다")
        void returnsZeroWithoutSideEffects() {
            given(mediaFileRepository.findOrphans(any())).willReturn(List.of());

            int result = executor.cleanup();

            assertThat(result).isZero();
            verify(mediaFileRepository, never()).deleteAll(any(Iterable.class));
            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    @Nested
    @DisplayName("tempKey가 모두 채워진 orphan 3건")
    class WhenThreeOrphansWithTempKeys {

        @Test
        @DisplayName("deleteAll 호출 + MediaFilesS3DeleteRequestedEvent에 3개 키 포함")
        void deletesAllAndPublishesEventWithThreeKeys() {
            List<MediaFile> orphans = List.of(
                    buildOrphan("temp/a.jpg"),
                    buildOrphan("temp/b.jpg"),
                    buildOrphan("temp/c.jpg")
            );
            given(mediaFileRepository.findOrphans(any())).willReturn(orphans);

            int result = executor.cleanup();

            assertThat(result).isEqualTo(3);
            verify(mediaFileRepository).deleteAll(orphans);
            verify(eventPublisher).publishEvent(
                    argThat((MediaFilesS3DeleteRequestedEvent event) ->
                            event.mediaKeys().size() == 3
                            && event.mediaKeys().containsAll(
                                    List.of("temp/a.jpg", "temp/b.jpg", "temp/c.jpg"))
                    )
            );
        }
    }

    @Nested
    @DisplayName("tempKey가 null인 row가 포함된 경우")
    class WhenSomeOrphansHaveNullTempKey {

        @Test
        @DisplayName("null tempKey는 S3 삭제 대상에서 제외된다")
        void excludesNullTempKeysFromEvent() {
            MediaFile withKey = buildOrphan("temp/exists.jpg");
            MediaFile withoutKey = MediaFile.builder()
                    .trip(Trip.builder()
                            .user(null)
                            .tripTitle("테스트여행")
                            .country("KR")
                            .startDate(LocalDate.of(2025, 1, 1))
                            .endDate(LocalDate.of(2025, 1, 10))
                            .hashtags("여행")
                            .build())
                    .mediaKey("media-key-no-temp")
                    .tempKey(null)
                    .build();
            List<MediaFile> orphans = List.of(withKey, withoutKey);
            given(mediaFileRepository.findOrphans(any())).willReturn(orphans);

            int result = executor.cleanup();

            assertThat(result).isEqualTo(2);
            verify(mediaFileRepository).deleteAll(orphans);
            verify(eventPublisher).publishEvent(
                    argThat((MediaFilesS3DeleteRequestedEvent event) ->
                            event.mediaKeys().equals(List.of("temp/exists.jpg"))
                    )
            );
        }
    }
}
