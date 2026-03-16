package com.triptyche.backend.global.config;

import com.triptyche.backend.domain.share.model.Share;
import com.triptyche.backend.domain.share.model.ShareStatus;
import com.triptyche.backend.domain.share.repository.ShareRepository;
import com.triptyche.backend.domain.trip.model.Trip;
import com.triptyche.backend.domain.trip.model.TripStatus;
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
 * K6 load test data initializer.
 * Runs only in local profile on server startup.
 *
 * Data created:
 * - test@test.com  : 10 Trips (CONFIRMED)
 * - test2@test.com : shared recipient of 5 trips from test (APPROVED)
 *
 * Idempotent: skips if data already exists.
 */
@Profile("local")
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

  private static final String TEST_USER_EMAIL = "test@test.com";
  private static final String TEST_USER2_EMAIL = "test2@test.com";
  private static final String PROVIDER = "google";
  private static final int TRIP_COUNT = 10;
  private static final int SHARED_TRIP_COUNT = 5;

  private final UserRepository userRepository;
  private final TripRepository tripRepository;
  private final ShareRepository shareRepository;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    log.info("[DataInitializer] K6 test data initialization start");

    User testUser = getOrCreateUser(TEST_USER_EMAIL, "testUser1", "test1");
    User testUser2 = getOrCreateUser(TEST_USER2_EMAIL, "testUser2", "test2");

    createTripsIfAbsent(testUser);
    createSharesIfAbsent(testUser, testUser2);

    log.info("[DataInitializer] Done — test: {} trips, test2: {} shared", TRIP_COUNT, SHARED_TRIP_COUNT);
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

    for (int i = 1; i <= TRIP_COUNT; i++) {
      Trip trip = Trip.builder()
              .user(owner)
              .tripTitle("Test Trip " + i)
              .country("Japan")
              .startDate(LocalDate.of(2024, 1, i))
              .endDate(LocalDate.of(2024, 1, i + 6))
              .hashtags("travel,test")
              .status(TripStatus.CONFIRMED)
              .build();
      tripRepository.save(trip);
    }
    log.info("[DataInitializer] {} trips created", TRIP_COUNT);
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
