package com.triptyche.backend.domain.trip.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.triptyche.backend.domain.share.model.Share;
import com.triptyche.backend.domain.share.model.ShareStatus;
import com.triptyche.backend.domain.trip.model.Trip;
import com.triptyche.backend.domain.trip.model.TripStatus;
import com.triptyche.backend.domain.user.model.User;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDate;
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
class TripRepositoryTest {

  @Autowired
  TripRepository tripRepository;

  @Autowired
  TestEntityManager tem;

  @Autowired
  EntityManagerFactory emf;

  private User owner;
  private User sharedUser;
  private User stranger;

  @BeforeEach
  void setUp() {
    owner = tem.persist(User.builder()
            .userName("owner")
            .userNickName("오너닉네임")
            .userEmail("owner@test.com")
            .provider("google")
            .build());

    sharedUser = tem.persist(User.builder()
            .userName("shared")
            .userNickName("공유닉네임")
            .userEmail("shared@test.com")
            .provider("google")
            .build());

    stranger = tem.persist(User.builder()
            .userName("stranger")
            .userNickName("타인닉네임")
            .userEmail("stranger@test.com")
            .provider("google")
            .build());

    tem.flush();
    tem.clear();
  }

  private Trip persistTrip(User user, TripStatus status, String title) {
    return tem.persist(Trip.builder()
            .user(user)
            .tripTitle(title)
            .country("KR")
            .startDate(LocalDate.of(2025, 1, 1))
            .endDate(LocalDate.of(2025, 1, 10))
            .hashtags("여행,서울")
            .status(status)
            .build());
  }

  private Share persistShare(Trip trip, Long recipientId, ShareStatus status) {
    return tem.persist(Share.builder()
            .trip(trip)
            .recipientId(recipientId)
            .shareStatus(status)
            .build());
  }

  private Statistics getStatistics() {
    SessionFactory sf = emf.unwrap(SessionFactory.class);
    sf.getStatistics().setStatisticsEnabled(true);
    return sf.getStatistics();
  }

  @Nested
  @DisplayName("findAllAccessibleTripsWithOwner()")
  class FindAllAccessibleTripsWithOwner {

    @Test
    @DisplayName("N+1 해결 검증: Trip 10개 + Share 5개 조회 시 쿼리 2회 이하")
    void nPlusOneResolved() {
      for (int i = 0; i < 10; i++) {
        persistTrip(owner, TripStatus.CONFIRMED, "여행" + i);
      }

      for (int i = 0; i < 5; i++) {
        Trip sharedTrip = persistTrip(stranger, TripStatus.CONFIRMED, "공유여행" + i);
        persistShare(sharedTrip, owner.getUserId(), ShareStatus.APPROVED);
      }

      tem.flush();
      tem.clear();

      Statistics stats = getStatistics();
      stats.clear();

      List<Trip> trips = tripRepository.findAllAccessibleTripsWithOwner(owner.getUserId());

      long queryCount = stats.getPrepareStatementCount();
      assertThat(queryCount).isLessThanOrEqualTo(2);
      assertThat(trips).hasSize(15);

      long countBeforeAccess = stats.getPrepareStatementCount();
      trips.forEach(trip -> trip.getUser().getUserNickName());
      long countAfterAccess = stats.getPrepareStatementCount();

      assertThat(countAfterAccess).isEqualTo(countBeforeAccess);
    }

    @Test
    @DisplayName("CONFIRMED, IMAGES_UPLOADED 상태만 반환되고 DRAFT는 제외")
    void onlyConfirmedAndImagesUploadedReturned() {
      persistTrip(owner, TripStatus.CONFIRMED, "확정여행");
      persistTrip(owner, TripStatus.IMAGES_UPLOADED, "업로드여행");
      persistTrip(owner, TripStatus.DRAFT, "초안여행");

      tem.flush();
      tem.clear();

      List<Trip> trips = tripRepository.findAllAccessibleTripsWithOwner(owner.getUserId());

      assertThat(trips).hasSize(2);
      assertThat(trips).extracting(Trip::getTripTitle)
              .containsExactlyInAnyOrder("확정여행", "업로드여행");
    }

    @Test
    @DisplayName("APPROVED 공유 여행만 반환, PENDING/REJECTED는 제외")
    void onlyApprovedSharesReturned() {
      Trip approvedTrip = persistTrip(stranger, TripStatus.CONFIRMED, "승인공유");
      Trip pendingTrip = persistTrip(stranger, TripStatus.CONFIRMED, "대기공유");
      Trip rejectedTrip = persistTrip(stranger, TripStatus.CONFIRMED, "거절공유");

      persistShare(approvedTrip, owner.getUserId(), ShareStatus.APPROVED);
      persistShare(pendingTrip, owner.getUserId(), ShareStatus.PENDING);
      persistShare(rejectedTrip, owner.getUserId(), ShareStatus.REJECTED);

      tem.flush();
      tem.clear();

      List<Trip> trips = tripRepository.findAllAccessibleTripsWithOwner(owner.getUserId());

      assertThat(trips).hasSize(1);
      assertThat(trips.get(0).getTripTitle()).isEqualTo("승인공유");
    }

    @Test
    @DisplayName("soft delete된 여행은 반환되지 않음")
    void softDeletedTripsExcluded() {
      Trip activeTrip = persistTrip(owner, TripStatus.CONFIRMED, "활성여행");
      Trip deletedTrip = persistTrip(owner, TripStatus.CONFIRMED, "삭제여행");
      deletedTrip.softDelete();

      tem.flush();
      tem.clear();

      List<Trip> trips = tripRepository.findAllAccessibleTripsWithOwner(owner.getUserId());

      assertThat(trips).hasSize(1);
      assertThat(trips.get(0).getTripTitle()).isEqualTo("활성여행");
    }
  }

}
