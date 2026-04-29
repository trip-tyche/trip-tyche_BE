package com.triptyche.backend.domain.trip.init;

import com.triptyche.backend.domain.media.model.MediaFile;
import com.triptyche.backend.domain.media.repository.MediaFileRepository;
import com.triptyche.backend.domain.share.model.Share;
import com.triptyche.backend.domain.share.model.ShareStatus;
import com.triptyche.backend.domain.share.repository.ShareRepository;
import com.triptyche.backend.domain.trip.model.PinPoint;
import com.triptyche.backend.domain.trip.model.Trip;
import com.triptyche.backend.domain.trip.model.TripStatus;
import com.triptyche.backend.domain.trip.repository.PinPointRepository;
import com.triptyche.backend.domain.trip.repository.TripRepository;
import com.triptyche.backend.domain.trip.service.PinPointService;
import com.triptyche.backend.domain.user.model.User;
import com.triptyche.backend.domain.user.repository.UserRepository;
import com.triptyche.backend.global.s3.S3KeyResolver;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로컬 개발용 시드 데이터: 도쿄·파리·뉴욕 3개 여행, 각 5개 핀포인트 + 미디어 파일.
 * local 프로파일에서만 실행. 멱등성 보장 — 데이터가 이미 존재하면 건너뜀.
 * 미디어 파일은 OCI에 seed/{city}/{filename}.webp 경로로 미리 업로드된 이미지를 참조.
 */
@Profile("local")
@Component
@RequiredArgsConstructor
@Slf4j
public class TripSeedInitializer implements ApplicationRunner {

  private static final String TEST_USER_EMAIL = "test@test.com";
  private static final String TEST_USER2_EMAIL = "test2@test.com";
  private static final String PROVIDER = "google";
  private static final int TRIP_COUNT = 3;
  private static final int SHARED_TRIP_COUNT = 2;

  private final UserRepository userRepository;
  private final TripRepository tripRepository;
  private final PinPointRepository pinPointRepository;
  private final ShareRepository shareRepository;
  private final MediaFileRepository mediaFileRepository;
  private final PinPointService pinPointService;
  private final S3KeyResolver s3KeyResolver;

  // ── Trip seed data ─────────────────────────────────────────────────────────

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

  // ── Media seed data ────────────────────────────────────────────────────────

  private record SeedMedia(
      String folder,
      String filename,
      double latitude,
      double longitude,
      LocalDateTime recordDate
  ) {}

  private static final Map<String, List<SeedMedia>> SEED_MEDIA = Map.of(
      "도쿄 벚꽃 여행", List.of(
          new SeedMedia("tokyo", "shibuya.webp",  35.6595, 139.7004, LocalDateTime.of(2024,  3, 28, 10, 30)),
          new SeedMedia("tokyo", "asakusa.webp",  35.7148, 139.7967, LocalDateTime.of(2024,  3, 29, 11,  0)),
          new SeedMedia("tokyo", "shinjuku.webp", 35.6852, 139.7100, LocalDateTime.of(2024,  3, 30, 14,  0)),
          new SeedMedia("tokyo", "skytree.webp",  35.7101, 139.8107, LocalDateTime.of(2024,  4,  1, 16, 30)),
          new SeedMedia("tokyo", "harajuku.webp", 35.6702, 139.7027, LocalDateTime.of(2024,  4,  2, 12,  0))
      ),
      "파리 로맨틱 여행", List.of(
          new SeedMedia("paris", "eiffel.webp",     48.8584, 2.2945, LocalDateTime.of(2024,  7, 14, 19,  0)),
          new SeedMedia("paris", "louvre.webp",     48.8606, 2.3376, LocalDateTime.of(2024,  7, 15, 10, 30)),
          new SeedMedia("paris", "notredame.webp",  48.8530, 2.3499, LocalDateTime.of(2024,  7, 16, 13,  0)),
          new SeedMedia("paris", "montmartre.webp", 48.8867, 2.3431, LocalDateTime.of(2024,  7, 18, 17, 30)),
          new SeedMedia("paris", "versailles.webp", 48.8049, 2.1204, LocalDateTime.of(2024,  7, 19, 11,  0))
      ),
      "뉴욕 가을 여행", List.of(
          new SeedMedia("newyork", "liberty.webp",     40.6892, -74.0445, LocalDateTime.of(2024, 10, 10, 10,  0)),
          new SeedMedia("newyork", "centralpark.webp", 40.7851, -73.9683, LocalDateTime.of(2024, 10, 11, 14,  0)),
          new SeedMedia("newyork", "timessquare.webp", 40.7580, -73.9855, LocalDateTime.of(2024, 10, 12, 20, 30)),
          new SeedMedia("newyork", "brooklyn.webp",    40.7061, -73.9969, LocalDateTime.of(2024, 10, 14, 11,  0)),
          new SeedMedia("newyork", "met.webp",         40.7794, -73.9632, LocalDateTime.of(2024, 10, 15, 15,  0))
      )
  );

  // ── Runner ─────────────────────────────────────────────────────────────────

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    log.info("[TripSeedInitializer] 로컬 시드 데이터 초기화 시작");

    User testUser  = getOrCreateUser(TEST_USER_EMAIL,  "testUser1", "test1");
    User testUser2 = getOrCreateUser(TEST_USER2_EMAIL, "testUser2", "test2");

    List<Trip> trips = getOrCreateTrips(testUser);
    trips.forEach(this::seedMediaIfAbsent);
    createSharesIfAbsent(testUser, testUser2);

    log.info("[TripSeedInitializer] 완료 — 여행 {}개 시드", TRIP_COUNT);
  }

  // ── Private helpers ────────────────────────────────────────────────────────

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
          log.info("[TripSeedInitializer] 사용자 생성: {}", email);
          return user;
        });
  }

  private List<Trip> getOrCreateTrips(User owner) {
    long existing = tripRepository.countByUserAndStatus(owner, TripStatus.CONFIRMED);
    if (existing >= TRIP_COUNT) {
      log.info("[TripSeedInitializer] 여행 이미 존재 ({}) — 생성 건너뜀", existing);
      return tripRepository.findAllAccessibleTripsWithOwner(owner.getUserId());
    }

    List<Trip> created = new ArrayList<>();
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
      created.add(trip);
      log.info("[TripSeedInitializer] 여행 '{}' 생성 완료 — 핀포인트 {}개", data.title(), data.pinPoints().size());
    }
    return created;
  }

  private void seedMediaIfAbsent(Trip trip) {
    List<MediaFile> existing = mediaFileRepository.findByTripTripId(trip.getTripId());
    if (!existing.isEmpty()) {
      log.info("[TripSeedInitializer] '{}' 미디어 이미 존재 — 건너뜀", trip.getTripTitle());
      return;
    }

    List<SeedMedia> seedList = SEED_MEDIA.get(trip.getTripTitle());
    if (seedList == null) {
      return;
    }

    List<PinPoint> pinPoints = pinPointService.findAllByTripId(trip.getTripId());
    for (SeedMedia seed : seedList) {
      String mediaKey  = "seed/" + seed.folder() + "/" + seed.filename();
      String mediaLink = s3KeyResolver.buildUrl(mediaKey);
      PinPoint pinPoint = pinPointService.assignPinPoint(pinPoints, trip, seed.latitude(), seed.longitude());

      MediaFile mediaFile = MediaFile.builder()
          .trip(trip)
          .pinPoint(pinPoint)
          .mediaType("image/webp")
          .mediaLink(mediaLink)
          .mediaKey(mediaKey)
          .recordDate(seed.recordDate())
          .latitude(seed.latitude())
          .longitude(seed.longitude())
          .build();
      mediaFileRepository.save(mediaFile);
    }
    log.info("[TripSeedInitializer] '{}' 미디어 파일 {}개 시드 완료", trip.getTripTitle(), seedList.size());
  }

  private void createSharesIfAbsent(User owner, User recipient) {
    List<Trip> trips = tripRepository.findAllAccessibleTripsWithOwner(owner.getUserId());
    if (trips.isEmpty()) {
      return;
    }

    long existingShares = shareRepository.findAllByRecipientId(recipient.getUserId()).size();
    if (existingShares >= SHARED_TRIP_COUNT) {
      log.info("[TripSeedInitializer] 공유 이미 존재 ({}) — 건너뜀", existingShares);
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
    log.info("[TripSeedInitializer] test2 공유 {}개 생성 완료", SHARED_TRIP_COUNT);
  }
}
