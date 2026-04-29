package com.triptyche.backend.domain.guest.service;

import com.triptyche.backend.domain.media.model.MediaFile;
import com.triptyche.backend.domain.media.repository.MediaFileRepository;
import com.triptyche.backend.domain.trip.model.PinPoint;
import com.triptyche.backend.domain.trip.model.Trip;
import com.triptyche.backend.domain.trip.model.TripStatus;
import com.triptyche.backend.domain.trip.repository.PinPointRepository;
import com.triptyche.backend.domain.trip.repository.TripRepository;
import com.triptyche.backend.domain.user.model.User;
import com.triptyche.backend.domain.user.repository.UserRepository;
import com.triptyche.backend.global.common.ResultCode;
import com.triptyche.backend.global.config.GuestProperties;
import com.triptyche.backend.global.exception.CustomException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuestTemplateCloneService {

    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final PinPointRepository pinPointRepository;
    private final MediaFileRepository mediaFileRepository;
    private final GuestProperties guestProperties;

    @Transactional
    public void cloneForGuest(User guestUser) {
        User templateUser = userRepository.findByUserEmail(guestProperties.templateEmail())
                .orElseThrow(() -> new CustomException(ResultCode.USER_NOT_FOUND));

        List<Trip> templateTrips = tripRepository.findAllByUser(templateUser);

        List<PinPoint> guestPins = new ArrayList<>();
        List<MediaFile> guestMediaFiles = new ArrayList<>();

        for (Trip templateTrip : templateTrips) {
            // shareTargetTripTitle 여행은 공유 신청 플로우로 제공 — 복사 제외
            if (guestProperties.shareTargetTripTitle().equals(templateTrip.getTripTitle())) {
                continue;
            }

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
                guestPins.add(guestPin);

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
                    guestMediaFiles.add(guestMf);
                }
            }
        }

        pinPointRepository.saveAll(guestPins);
        mediaFileRepository.saveAll(guestMediaFiles);
        log.info("게스트 데모 데이터 복사 완료 (도쿄·파리): {}", guestUser.getUserEmail());
    }
}
