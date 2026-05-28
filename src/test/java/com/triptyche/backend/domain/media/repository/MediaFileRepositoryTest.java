package com.triptyche.backend.domain.media.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import com.triptyche.backend.domain.media.model.MediaFile;
import com.triptyche.backend.domain.media.model.ProcessingStatus;
import com.triptyche.backend.domain.trip.model.PinPoint;
import com.triptyche.backend.domain.trip.model.Trip;
import com.triptyche.backend.domain.trip.model.TripStatus;
import com.triptyche.backend.domain.user.model.User;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest
class MediaFileRepositoryTest {

    @Autowired
    MediaFileRepository mediaFileRepository;

    @Autowired
    TestEntityManager tem;

    @Autowired
    EntityManagerFactory emf;

    private User owner;
    private Trip trip;
    private Trip otherTrip;
    private PinPoint pinPoint;

    @BeforeEach
    void setUp() {
        owner = tem.persist(User.builder()
                .userName("owner")
                .userNickName("오너닉네임")
                .userEmail("owner@test.com")
                .provider("google")
                .build());

        trip = tem.persist(Trip.builder()
                .user(owner)
                .tripTitle("여행A")
                .country("KR")
                .startDate(LocalDate.of(2025, 1, 1))
                .endDate(LocalDate.of(2025, 1, 10))
                .hashtags("여행")
                .status(TripStatus.CONFIRMED)
                .build());

        otherTrip = tem.persist(Trip.builder()
                .user(owner)
                .tripTitle("여행B")
                .country("KR")
                .startDate(LocalDate.of(2025, 2, 1))
                .endDate(LocalDate.of(2025, 2, 10))
                .hashtags("여행")
                .status(TripStatus.CONFIRMED)
                .build());

        pinPoint = tem.persist(PinPoint.builder()
                .trip(trip)
                .latitude(37.5)
                .longitude(127.0)
                .build());

        tem.flush();
        tem.clear();
    }

    private MediaFile persistMediaFile(Trip t, PinPoint pp, String mediaKey) {
        return tem.persist(MediaFile.builder()
                .trip(t)
                .pinPoint(pp)
                .mediaKey(mediaKey)
                .mediaType("image/jpeg")
                .mediaLink("https://example.com/" + mediaKey)
                .recordDate(LocalDateTime.of(2025, 1, 5, 12, 0))
                .latitude(37.5)
                .longitude(127.0)
                .build());
    }

    private Statistics getStatistics() {
        SessionFactory sf = emf.unwrap(SessionFactory.class);
        sf.getStatistics().setStatisticsEnabled(true);
        return sf.getStatistics();
    }

    @Nested
    @DisplayName("findOrphans()")
    class FindOrphans {

        private MediaFile persistOrphan(String mediaKey, LocalDateTime createdAt, ProcessingStatus status) {
            return persistRow(mediaKey, "temp/" + mediaKey, createdAt, status);
        }

        private MediaFile persistRow(String mediaKey, String tempKey, LocalDateTime createdAt,
                ProcessingStatus status) {
            MediaFile mf = MediaFile.builder()
                    .trip(trip)
                    .mediaKey(mediaKey)
                    .tempKey(tempKey)
                    .mediaType("image/jpeg")
                    .processingStatus(status)
                    .build();
            MediaFile persisted = tem.persist(mf);
            // @CreationTimestamp는 JPA insert 시 자동 설정되므로 JPQL로 createdAt을 직접 덮어씀
            tem.getEntityManager()
                    .createQuery("UPDATE MediaFile m SET m.createdAt = :ts WHERE m.mediaFileId = :id")
                    .setParameter("ts", createdAt)
                    .setParameter("id", persisted.getMediaFileId())
                    .executeUpdate();
            return persisted;
        }

        @Test
        @DisplayName("v1 legacy row(tempKey=null, status=LEGACY)는 cleanup 대상에서 제외")
        void excludesLegacyStatusRowWithoutTempKey() {
            LocalDateTime threshold = LocalDateTime.now();
            persistRow("v1-legacy", null, threshold.minusHours(7), ProcessingStatus.LEGACY);
            tem.flush();
            tem.clear();

            List<MediaFile> result = mediaFileRepository.findOrphans(threshold);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("v1 row(tempKey=null, status=null)도 tempKey 가드로 제외 (Defense in Depth)")
        void excludesNullStatusRowWithoutTempKey() {
            LocalDateTime threshold = LocalDateTime.now();
            persistRow("v1-null", null, threshold.minusHours(7), null);
            tem.flush();
            tem.clear();

            List<MediaFile> result = mediaFileRepository.findOrphans(threshold);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("v2 orphan(tempKey 채워짐, status=null, threshold 이전)은 cleanup 대상으로 포함")
        void includesV2OrphanWithTempKey() {
            LocalDateTime threshold = LocalDateTime.now();
            persistRow("v2-orphan", "temp/v2-orphan", threshold.minusHours(7), null);
            tem.flush();
            tem.clear();

            List<MediaFile> result = mediaFileRepository.findOrphans(threshold);

            assertThat(result).extracting(MediaFile::getMediaKey)
                    .containsExactly("v2-orphan");
        }

        @Test
        @DisplayName("threshold 이전 + status null → orphan으로 포함")
        void includesNullStatusBeforeThreshold() {
            LocalDateTime threshold = LocalDateTime.now();
            persistOrphan("old-null", threshold.minusHours(7), null);
            tem.flush();
            tem.clear();

            List<MediaFile> result = mediaFileRepository.findOrphans(threshold);

            assertThat(result).extracting(MediaFile::getMediaKey)
                    .containsExactly("old-null");
        }

        @Test
        @DisplayName("threshold 이후 + status null → 아직 orphan 아님, 제외")
        void excludesNullStatusAfterThreshold() {
            LocalDateTime threshold = LocalDateTime.now();
            persistOrphan("new-null", threshold.plusHours(1), null);
            tem.flush();
            tem.clear();

            List<MediaFile> result = mediaFileRepository.findOrphans(threshold);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("threshold 이전 + status UPLOADED → 정상 흐름 진입, 제외")
        void excludesUploadedStatusBeforeThreshold() {
            LocalDateTime threshold = LocalDateTime.now();
            persistOrphan("old-uploaded", threshold.minusHours(7), ProcessingStatus.UPLOADED);
            tem.flush();
            tem.clear();

            List<MediaFile> result = mediaFileRepository.findOrphans(threshold);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByTripTripIdWithPinPoint()")
    class FindByTripTripIdWithPinPoint {

        @Test
        @DisplayName("주어진 tripId의 MediaFile들이 pinPoint까지 즉시 로딩됨 (N+1 없음)")
        void pinPointFetchedWithoutExtraQuery() {
            persistMediaFile(trip, pinPoint, "key1");
            persistMediaFile(trip, pinPoint, "key2");
            persistMediaFile(trip, pinPoint, "key3");
            tem.flush();
            tem.clear();

            Statistics stats = getStatistics();
            stats.clear();

            List<MediaFile> results = mediaFileRepository.findByTripTripIdWithPinPoint(trip.getTripId());

            long queryCountAfterLoad = stats.getPrepareStatementCount();

            // pinPoint 접근 시 추가 쿼리 발생 없어야 함
            assertThatNoException().isThrownBy(() ->
                    results.forEach(m -> m.getPinPoint().getLatitude())
            );

            long queryCountAfterAccess = stats.getPrepareStatementCount();
            assertThat(queryCountAfterAccess).isEqualTo(queryCountAfterLoad);
            assertThat(results).hasSize(3);
        }

        @Test
        @DisplayName("다른 trip의 MediaFile은 결과에 포함되지 않음")
        void otherTripRowsExcluded() {
            PinPoint otherPinPoint = tem.persist(PinPoint.builder()
                    .trip(otherTrip)
                    .latitude(35.0)
                    .longitude(129.0)
                    .build());

            persistMediaFile(trip, pinPoint, "trip-key1");
            persistMediaFile(trip, pinPoint, "trip-key2");
            persistMediaFile(otherTrip, otherPinPoint, "other-key1");
            tem.flush();
            tem.clear();

            List<MediaFile> results = mediaFileRepository.findByTripTripIdWithPinPoint(trip.getTripId());

            assertThat(results).hasSize(2);
            assertThat(results).extracting(MediaFile::getMediaKey)
                    .containsExactlyInAnyOrder("trip-key1", "trip-key2");
        }
    }
}
