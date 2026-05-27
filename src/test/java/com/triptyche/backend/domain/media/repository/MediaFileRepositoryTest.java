package com.triptyche.backend.domain.media.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import com.triptyche.backend.domain.media.model.MediaFile;
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
