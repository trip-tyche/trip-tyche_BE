package com.triptyche.backend.global.config;

import com.triptyche.backend.domain.media.model.MediaFile;
import com.triptyche.backend.domain.media.repository.MediaFileRepository;
import com.triptyche.backend.domain.trip.model.PinPoint;
import com.triptyche.backend.domain.trip.model.Trip;
import com.triptyche.backend.domain.trip.model.TripStatus;
import com.triptyche.backend.domain.trip.repository.PinPointRepository;
import com.triptyche.backend.domain.trip.repository.TripRepository;
import com.triptyche.backend.domain.user.model.User;
import com.triptyche.backend.domain.user.model.UserRole;
import com.triptyche.backend.domain.user.repository.UserRepository;
import com.triptyche.backend.global.s3.S3UploadService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class GuestTemplateInitializer implements ApplicationRunner {

  private static final String PROVIDER = "guest_template";
  private static final String MEDIA_TYPE = "image/webp";

  private final GuestProperties guestProperties;
  private final S3UploadService s3UploadService;
  private final UserRepository userRepository;
  private final TripRepository tripRepository;
  private final PinPointRepository pinPointRepository;
  private final MediaFileRepository mediaFileRepository;

  private record PinData(double latitude, double longitude, String mediaKey) {}

  private record TripData(
      String title,
      String country,
      LocalDate startDate,
      LocalDate endDate,
      String hashtags,
      List<PinData> pins
  ) {}

  private static final List<TripData> SEED_TRIPS = List.of(
      new TripData(
          "도쿄 벚꽃 여행",
          "🇯🇵/일본/JAPAN",
          LocalDate.of(2024, 3, 28),
          LocalDate.of(2024, 4, 2),
          "벚꽃,봄여행,일본",
          List.of(
              new PinData(35.6595, 139.7004, "seed/tokyo/shibuya.webp"),
              new PinData(35.7148, 139.7967, "seed/tokyo/asakusa.webp"),
              new PinData(35.6852, 139.7100, "seed/tokyo/shinjuku.webp"),
              new PinData(35.7101, 139.8107, "seed/tokyo/skytree.webp"),
              new PinData(35.6702, 139.7027, "seed/tokyo/harajuku.webp")
          )
      ),
      new TripData(
          "파리 로맨틱 여행",
          "🇫🇷/프랑스/FRANCE",
          LocalDate.of(2024, 7, 14),
          LocalDate.of(2024, 7, 20),
          "에펠탑,유럽여행,파리",
          List.of(
              new PinData(48.8584, 2.2945, "seed/paris/eiffel.webp"),
              new PinData(48.8606, 2.3376, "seed/paris/louvre.webp"),
              new PinData(48.8530, 2.3499, "seed/paris/notredame.webp"),
              new PinData(48.8867, 2.3431, "seed/paris/montmartre.webp"),
              new PinData(48.8049, 2.1204, "seed/paris/versailles.webp")
          )
      ),
      new TripData(
          "뉴욕 가을 여행",
          "🇺🇸/미국/USA",
          LocalDate.of(2024, 10, 10),
          LocalDate.of(2024, 10, 16),
          "뉴욕,자유의여신상,미국여행",
          List.of(
              new PinData(40.6892, -74.0445, "seed/newyork/liberty.webp"),
              new PinData(40.7851, -73.9683, "seed/newyork/centralpark.webp"),
              new PinData(40.7580, -73.9855, "seed/newyork/timessquare.webp"),
              new PinData(40.7061, -73.9969, "seed/newyork/brooklyn.webp"),
              new PinData(40.7794, -73.9632, "seed/newyork/met.webp")
          )
      )
  );

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    log.info("[GuestTemplateInitializer] 게스트 템플릿 시드 초기화 시작");

    User templateUser = userRepository.findByUserEmail(guestProperties.templateEmail())
        .orElseGet(() -> {
          User user = User.builder()
              .userEmail(guestProperties.templateEmail())
              .userName("데모계정")
              .userNickName("데모계정")
              .provider(PROVIDER)
              .role(UserRole.USER)
              .build();
          userRepository.save(user);
          log.info("[GuestTemplateInitializer] 템플릿 계정 생성: {}", guestProperties.templateEmail());
          return user;
        });

    if (!tripRepository.findAllByUser(templateUser).isEmpty()) {
      log.info("[GuestTemplateInitializer] 템플릿 여행 데이터가 이미 존재함 — skip");
      return;
    }

    for (TripData data : SEED_TRIPS) {
      Trip trip = Trip.builder()
          .user(templateUser)
          .tripTitle(data.title())
          .country(data.country())
          .startDate(data.startDate())
          .endDate(data.endDate())
          .hashtags(data.hashtags())
          .status(TripStatus.CONFIRMED)
          .build();
      tripRepository.save(trip);

      List<PinData> pins = data.pins();
      for (int i = 0; i < pins.size(); i++) {
        PinData pin = pins.get(i);

        PinPoint pinPoint = PinPoint.builder()
            .trip(trip)
            .latitude(pin.latitude())
            .longitude(pin.longitude())
            .build();
        pinPointRepository.save(pinPoint);

        String mediaKey = pin.mediaKey();
        MediaFile mediaFile = MediaFile.builder()
            .trip(trip)
            .pinPoint(pinPoint)
            .mediaType(MEDIA_TYPE)
            .mediaKey(mediaKey)
            .mediaLink(s3UploadService.buildUrl(mediaKey))
            .latitude(pin.latitude())
            .longitude(pin.longitude())
            .recordDate(LocalDateTime.of(data.startDate().plusDays(i), LocalTime.NOON))
            .build();
        mediaFileRepository.save(mediaFile);
      }
      log.info("[GuestTemplateInitializer] 여행 '{}' 생성 완료 — 핀포인트 {}개 + 미디어 파일 {}개", data.title(), pins.size(), pins.size());
    }

    log.info("[GuestTemplateInitializer] 완료 — 여행 3개, 핀포인트 15개, 미디어 파일 15개 생성");
  }
}
