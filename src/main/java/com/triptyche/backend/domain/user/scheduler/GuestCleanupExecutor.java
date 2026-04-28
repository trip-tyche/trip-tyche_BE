package com.triptyche.backend.domain.user.scheduler;

import com.triptyche.backend.domain.media.model.MediaFile;
import com.triptyche.backend.domain.media.repository.MediaFileRepository;
import com.triptyche.backend.domain.notification.repository.NotificationRepository;
import com.triptyche.backend.domain.share.repository.ShareRepository;
import com.triptyche.backend.domain.trip.model.Trip;
import com.triptyche.backend.domain.trip.repository.PinPointRepository;
import com.triptyche.backend.domain.trip.repository.TripRepository;
import com.triptyche.backend.domain.user.model.User;
import com.triptyche.backend.domain.user.repository.UserRepository;
import com.triptyche.backend.global.redis.GuestShareQueueRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class GuestCleanupExecutor {

    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final ShareRepository shareRepository;
    private final MediaFileRepository mediaFileRepository;
    private final PinPointRepository pinPointRepository;
    private final NotificationRepository notificationRepository;
    private final GuestShareQueueRepository guestShareQueueRepository;

    @Transactional
    public List<String> deleteExpiredGuests(List<User> expiredGuests) {
        List<Long> userIds = expiredGuests.stream().map(User::getUserId).toList();
        // @SQLRestriction 적용 — soft-delete된 게스트 Trip의 S3 키는 수집 제외 (기존 동작 유지)
        List<Trip> allTrips = tripRepository.findAllByUserIn(expiredGuests);

        List<String> s3Keys = allTrips.isEmpty() ? List.of() :
                mediaFileRepository.findAllByTripIn(allTrips).stream()
                        .map(MediaFile::getMediaKey)
                        .filter(key -> !key.startsWith("seed/"))
                        .toList();

        // DB 삭제 순서 (FK 제약 준수)
        notificationRepository.deleteAllByUserIdIn(userIds);
        if (!allTrips.isEmpty()) {
            shareRepository.deleteAllByTripIn(allTrips);
            mediaFileRepository.deleteAllByTripIn(allTrips);
            pinPointRepository.deleteAllByTripIn(allTrips);
            // JPQL bulk delete — @SQLRestriction 우회, soft-deleted Trip도 포함 삭제 (의도된 동작)
            tripRepository.deleteAllByUserIn(expiredGuests);
        }
        guestShareQueueRepository.removeAll(userIds);
        userRepository.deleteAllByUserIdIn(userIds);

        log.info("만료된 게스트 계정 정리 완료 — {}건", expiredGuests.size());
        return s3Keys;
    }
}