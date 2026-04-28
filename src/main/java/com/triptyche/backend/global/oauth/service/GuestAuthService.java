package com.triptyche.backend.global.oauth.service;

import com.triptyche.backend.domain.media.model.MediaFile;
import com.triptyche.backend.domain.media.repository.MediaFileRepository;
import com.triptyche.backend.domain.trip.model.PinPoint;
import com.triptyche.backend.domain.trip.model.Trip;
import com.triptyche.backend.domain.trip.model.TripStatus;
import com.triptyche.backend.domain.trip.repository.PinPointRepository;
import com.triptyche.backend.domain.trip.repository.TripRepository;
import com.triptyche.backend.domain.user.model.User;
import com.triptyche.backend.domain.user.repository.UserRepository;
import com.triptyche.backend.domain.user.service.UserService;
import com.triptyche.backend.global.common.ResultCode;
import com.triptyche.backend.global.config.JwtProperties;
import com.triptyche.backend.global.exception.CustomException;
import com.triptyche.backend.global.redis.GuestShareQueueRepository;
import com.triptyche.backend.global.util.CookieUtil;
import com.triptyche.backend.global.util.JwtTokenProvider;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuestAuthService {

    private static final String GUEST_PROVIDER = "guest";
    private static final String TEMPLATE_EMAIL = "guest_template@triptyche.com";

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final CookieUtil cookieUtil;
    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final PinPointRepository pinPointRepository;
    private final MediaFileRepository mediaFileRepository;
    private final GuestShareQueueRepository guestShareQueueRepository;

    @Transactional
    public String issueGuestToken(HttpServletResponse response) {
        User guestUser = userService.createGuestUser();

        copyDemoDataFromTemplate(guestUser);
        guestShareQueueRepository.enqueue(guestUser.getUserId());

        String guestEmail = guestUser.getUserEmail();
        String accessToken = jwtTokenProvider.createGuestToken(guestEmail, GUEST_PROVIDER);

        cookieUtil.setCookie(response, "access_token", accessToken,
                (int) jwtProperties.guestTokenExpirySeconds());

        log.info("게스트 계정 생성: {}", guestEmail);
        return accessToken;
    }

    private void copyDemoDataFromTemplate(User guestUser) {
        User templateUser = userRepository.findByUserEmail(TEMPLATE_EMAIL)
                .orElseThrow(() -> new CustomException(ResultCode.USER_NOT_FOUND));

        List<Trip> templateTrips = tripRepository.findAllByUser(templateUser);

        for (Trip templateTrip : templateTrips) {
            Trip guestTrip = Trip.builder()
                    .user(guestUser)
                    .tripTitle(templateTrip.getTripTitle())
                    .country(templateTrip.getCountry())
                    .startDate(templateTrip.getStartDate())
                    .endDate(templateTrip.getEndDate())
                    .hashtags(templateTrip.getHashtags())
                    .status(TripStatus.CONFIRMED)
                    .build();
            tripRepository.save(guestTrip);

            List<PinPoint> templatePins = pinPointRepository.findByTripTripId(templateTrip.getTripId());

            for (PinPoint templatePin : templatePins) {
                PinPoint guestPin = PinPoint.builder()
                        .trip(guestTrip)
                        .latitude(templatePin.getLatitude())
                        .longitude(templatePin.getLongitude())
                        .build();
                pinPointRepository.save(guestPin);

                for (MediaFile templateMf : templatePin.getMediaFiles()) {
                    MediaFile guestMf = MediaFile.builder()
                            .trip(guestTrip)
                            .pinPoint(guestPin)
                            .mediaType(templateMf.getMediaType())
                            .mediaLink(templateMf.getMediaLink())
                            .mediaKey(templateMf.getMediaKey())
                            .recordDate(templateMf.getRecordDate())
                            .latitude(templateMf.getLatitude())
                            .longitude(templateMf.getLongitude())
                            .build();
                    mediaFileRepository.save(guestMf);
                }
            }
        }
        log.info("게스트 데모 데이터 복사 완료: {}", guestUser.getUserEmail());
    }
}