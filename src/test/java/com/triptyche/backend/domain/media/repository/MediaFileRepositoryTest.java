package com.triptyche.backend.domain.media.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import com.triptyche.backend.domain.media.dto.TripCoverRow;
import com.triptyche.backend.domain.media.dto.TripPhotoCountRow;
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
import java.util.Map;
import java.util.stream.Collectors;
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

    @Nested
    @DisplayName("findCoversByTripIds() / countByTripIds()")
    class TripCovers {

        private static final LocalDateTime DEFAULT_DATE = LocalDateTime.of(1980, 1, 1, 0, 0, 0);

        private MediaFile persistPhoto(Trip t, String mediaKey, String mediaLink, LocalDateTime recordDate) {
            return tem.persist(MediaFile.builder()
                    .trip(t)
                    .mediaKey(mediaKey)
                    .mediaType("image/jpeg")
                    .mediaLink(mediaLink)
                    .recordDate(recordDate)
                    .latitude(37.5)
                    .longitude(127.0)
                    .build());
        }

        private Map<Long, String> coversOf(List<Long> tripIds) {
            return mediaFileRepository.findCoversByTripIds(tripIds, DEFAULT_DATE).stream()
                    .collect(Collectors.toMap(TripCoverRow::getTripId, TripCoverRow::getMediaLink));
        }

        private Map<Long, Long> countsOf(List<Long> tripIds) {
            return mediaFileRepository.countByTripIds(tripIds).stream()
                    .collect(Collectors.toMap(TripPhotoCountRow::getTripId, TripPhotoCountRow::getPhotoCount));
        }

        @Test
        @DisplayName("1980 기본 날짜 사진은 가장 이른 날짜여도 표지로 뽑히지 않는다")
        void defaultDateIsPushedBack() {
            persistPhoto(trip, "k-default", "link-default", DEFAULT_DATE);
            persistPhoto(trip, "k-late", "link-late", LocalDateTime.of(2025, 1, 9, 10, 0));
            persistPhoto(trip, "k-early", "link-early", LocalDateTime.of(2025, 1, 2, 10, 0));
            tem.flush();
            tem.clear();

            assertThat(coversOf(List.of(trip.getTripId())))
                    .containsExactly(Map.entry(trip.getTripId(), "link-early"));
        }

        @Test
        @DisplayName("표지 행의 thumbnail_key를 함께 돌려준다")
        void returnsThumbnailKey() {
            MediaFile mf = persistPhoto(trip, "k-1", "link-1", LocalDateTime.of(2025, 1, 2, 10, 0));
            mf.updateThumbnailKey("processed/1/k-1_thumb.webp");
            tem.flush();
            tem.clear();

            assertThat(mediaFileRepository.findCoversByTripIds(List.of(trip.getTripId()), DEFAULT_DATE))
                    .singleElement()
                    .extracting(TripCoverRow::getThumbnailKey)
                    .isEqualTo("processed/1/k-1_thumb.webp");
        }

        @Test
        @DisplayName("날짜 있는 사진이 없으면 기본 날짜 사진이라도 표지가 된다")
        void onlyDefaultDatePhoto() {
            persistPhoto(trip, "k-default", "link-default", DEFAULT_DATE);
            tem.flush();
            tem.clear();

            assertThat(coversOf(List.of(trip.getTripId())))
                    .containsExactly(Map.entry(trip.getTripId(), "link-default"));
        }

        @Test
        @DisplayName("같은 날짜면 media_file_id가 작은 사진이 표지다")
        void tieBreakByMediaFileId() {
            LocalDateTime same = LocalDateTime.of(2025, 1, 3, 9, 0);
            MediaFile first = persistPhoto(trip, "k-1", "link-1", same);
            persistPhoto(trip, "k-2", "link-2", same);
            tem.flush();
            tem.clear();

            assertThat(coversOf(List.of(trip.getTripId())))
                    .containsExactly(Map.entry(trip.getTripId(), first.getMediaLink()));
        }

        @Test
        @DisplayName("처리 중 사진(media_link=null)은 표지에서 빠지지만 사진 수에는 포함된다")
        void processingPhotoCountedButNotCover() {
            persistPhoto(trip, "k-processing", null, LocalDateTime.of(2025, 1, 1, 8, 0));
            persistPhoto(trip, "k-processing2", null, null);
            tem.flush();
            tem.clear();

            assertThat(coversOf(List.of(trip.getTripId()))).isEmpty();
            assertThat(countsOf(List.of(trip.getTripId())))
                    .containsExactly(Map.entry(trip.getTripId(), 2L));
        }

        @Test
        @DisplayName("여러 여행을 한 번에 조회하고 사진 없는 여행은 결과에 없다")
        void multipleTrips() {
            Trip emptyTrip = tem.persist(Trip.builder()
                    .user(owner)
                    .tripTitle("여행C")
                    .country("KR")
                    .startDate(LocalDate.of(2025, 3, 1))
                    .endDate(LocalDate.of(2025, 3, 2))
                    .hashtags("여행")
                    .status(TripStatus.CONFIRMED)
                    .build());
            persistPhoto(trip, "a-1", "link-a1", LocalDateTime.of(2025, 1, 5, 0, 0));
            persistPhoto(trip, "a-2", "link-a2", LocalDateTime.of(2025, 1, 4, 0, 0));
            persistPhoto(otherTrip, "b-1", "link-b1", LocalDateTime.of(2025, 2, 5, 0, 0));
            tem.flush();
            tem.clear();

            List<Long> tripIds = List.of(trip.getTripId(), otherTrip.getTripId(), emptyTrip.getTripId());

            assertThat(coversOf(tripIds)).containsOnly(
                    Map.entry(trip.getTripId(), "link-a2"),
                    Map.entry(otherTrip.getTripId(), "link-b1"));
            assertThat(countsOf(tripIds)).containsOnly(
                    Map.entry(trip.getTripId(), 2L),
                    Map.entry(otherTrip.getTripId(), 1L));
        }
    }
}
