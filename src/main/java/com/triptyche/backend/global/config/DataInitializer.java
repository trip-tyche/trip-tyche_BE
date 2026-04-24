package com.triptyche.backend.global.config;

import com.triptyche.backend.domain.share.model.Share;
import com.triptyche.backend.domain.share.model.ShareStatus;
import com.triptyche.backend.domain.share.repository.ShareRepository;
import com.triptyche.backend.domain.trip.model.PinPoint;
import com.triptyche.backend.domain.trip.model.Trip;
import com.triptyche.backend.domain.trip.model.TripStatus;
import com.triptyche.backend.domain.trip.repository.PinPointRepository;
import com.triptyche.backend.domain.trip.repository.TripRepository;
import com.triptyche.backend.domain.user.model.User;
import com.triptyche.backend.domain.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Local dev seed data: 3 realistic trips (Tokyo / Paris / New York), each with 5 pinpoints.
 * Runs only in local profile. Idempotent — skips if data already exists.
 */
@Profile("local")
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

  private static final String TEST_USER_EMAIL = "test@test.com";
  private static final String TEST_USER2_EMAIL = "test2@test.com";
  private static final String PROVIDER = "google";
  private static final int TRIP_COUNT = 3;
  private static final int SHARED_TRIP_COUNT = 2;

  private final UserRepository userRepository;
  private final TripRepository tripRepository;
  private final PinPointRepository pinPointRepository;
  private final ShareRepository shareRepository;

  private record TripData(
      String title,
      String country,
      LocalDate startDate,
      LocalDate endDate,
      String hashtags,
      List<double[]> pinPoints
  ) {}

  private static final List<TripData> SEED_TRIPS = List.of(
      new TripData(
          "도쿄 벚꽃 여행",
          "🇯🇵/일본/JAPAN",
          LocalDate.of(2024, 3, 28),
          LocalDate.of(2024, 4, 2),
          "벚꽃,봄여행,일본",
          List.of(
              new double[]{35.6595, 139.7004},   // 시부야 스크램블 교차로
              new double[]{35.7148, 139.7967},   // 아사쿠사 센소지
              new double[]{35.6852, 139.7100},   // 신주쿠 교엔
              new double[]{35.7101, 139.8107},   // 도쿄 스카이트리
              new double[]{35.6702, 139.7027}    // 하라주쿠 다케시타 거리
          )
      ),
      new TripData(
          "파리 로맨틱 여행",
          "🇫🇷/프랑스/FRANCE",
          LocalDate.of(2024, 7, 14),
          LocalDate.of(2024, 7, 20),
          "에펠탑,유럽여행,파리",
          List.of(
              new double[]{48.8584, 2.2945},    // 에펠탑
              new double[]{48.8606, 2.3376},    // 루브르 박물관
              new double[]{48.8530, 2.3499},    // 노트르담 대성당
              new double[]{48.8867, 2.3431},    // 몽마르트르 사크레쾨르
              new double[]{48.8049, 2.1204}     // 베르사유 궁전
          )
      ),
      new TripData(
          "뉴욕 가을 여행",
          "🇺🇸/미국/USA",
          LocalDate.of(2024, 10, 10),
          LocalDate.of(2024, 10, 16),
          "뉴욕,자유의여신상,미국여행",
          List.of(
              new double[]{40.6892, -74.0445},  // 자유의 여신상
              new double[]{40.7851, -73.9683},  // 센트럴 파크
              new double[]{40.7580, -73.9855},  // 타임스 스퀘어
              new double[]{40.7061, -73.9969},  // 브루클린 브리지
              new double[]{40.7794, -73.9632}   // 메트로폴리탄 미술관
          )
      )
  );

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    log.info("[DataInitializer] Local seed data initialization start");

    User testUser = getOrCreateUser(TEST_USER_EMAIL, "testUser1", "test1");
    User testUser2 = getOrCreateUser(TEST_USER2_EMAIL, "testUser2", "test2");

    createTripsIfAbsent(testUser);
    createSharesIfAbsent(testUser, testUser2);

    log.info("[DataInitializer] Done — {} trips with pinpoints seeded", TRIP_COUNT);
  }

  private User getOrCreateUser(String email, String name, String nickname) {
    return userRepository.findByUserEmail(email)
        .orElseGet(() -> {
          User user = User.builder()
              .userEmail(email)
              .userName(name)
              .userNickName(nickname)
              .provider(PROVIDER)
              .build();
          userRepository.save(user);
          log.info("[DataInitializer] User created: {}", email);
          return user;
        });
  }

  private void createTripsIfAbsent(User owner) {
    long existing = tripRepository.countByUserAndStatus(owner, TripStatus.CONFIRMED);
    if (existing >= TRIP_COUNT) {
      log.info("[DataInitializer] Trips already exist ({}) — skip", existing);
      return;
    }

    for (TripData data : SEED_TRIPS) {
      Trip trip = Trip.builder()
          .user(owner)
          .tripTitle(data.title())
          .country(data.country())
          .startDate(data.startDate())
          .endDate(data.endDate())
          .hashtags(data.hashtags())
          .status(TripStatus.CONFIRMED)
          .build();
      tripRepository.save(trip);

      for (double[] coord : data.pinPoints()) {
        PinPoint pinPoint = PinPoint.builder()
            .trip(trip)
            .latitude(coord[0])
            .longitude(coord[1])
            .build();
        pinPointRepository.save(pinPoint);
      }
      log.info("[DataInitializer] Trip '{}' created with {} pinpoints", data.title(), data.pinPoints().size());
    }
  }

  private void createSharesIfAbsent(User owner, User recipient) {
    List<Trip> trips = tripRepository.findAllAccessibleTripsWithOwner(owner.getUserId());
    if (trips.isEmpty()) {
      return;
    }

    long existingShares = shareRepository.findAllByRecipientId(recipient.getUserId()).size();
    if (existingShares >= SHARED_TRIP_COUNT) {
      log.info("[DataInitializer] Shares already exist ({}) — skip", existingShares);
      return;
    }

    trips.stream()
        .limit(SHARED_TRIP_COUNT)
        .forEach(trip -> {
          boolean exists = shareRepository.existsByTripAndRecipientId(trip, recipient.getUserId());
          if (!exists) {
            Share share = Share.builder()
                .trip(trip)
                .recipientId(recipient.getUserId())
                .shareStatus(ShareStatus.APPROVED)
                .build();
            shareRepository.save(share);
          }
        });
    log.info("[DataInitializer] {} shares created for test2", SHARED_TRIP_COUNT);
  }
}
