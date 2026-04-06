package com.triptyche.backend.domain.user.scheduler;

import com.triptyche.backend.domain.media.repository.MediaFileRepository;
import com.triptyche.backend.domain.trip.model.Trip;
import com.triptyche.backend.domain.trip.repository.TripRepository;
import com.triptyche.backend.domain.user.model.User;
import com.triptyche.backend.domain.user.model.UserRole;
import com.triptyche.backend.domain.user.repository.UserRepository;
import com.triptyche.backend.global.s3.S3UploadService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class GuestCleanupScheduler {

    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final MediaFileRepository mediaFileRepository;
    private final S3UploadService s3UploadService;

    private static final int GUEST_SESSION_HOURS = 4;

    @Scheduled(cron = "0 0 * * * *") // 매 정시 실행
    @Transactional
    public void cleanupExpiredGuests() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(GUEST_SESSION_HOURS);
        List<User> expiredGuests = userRepository.findByRoleAndCreatedAtBefore(UserRole.GUEST, threshold);

        if (expiredGuests.isEmpty()) {
            log.debug("만료된 게스트 계정 없음 — 정리 생략");
            return;
        }

        for (User guest : expiredGuests) {
            List<Trip> trips = tripRepository.findAllByUser(guest);

            List<String> deletableKeys = mediaFileRepository.findAllByTripIn(trips).stream()
                    .map(mf -> mf.getMediaKey())
                    .filter(key -> !key.startsWith("demo/"))
                    .toList();

            if (!deletableKeys.isEmpty()) {
                try {
                    s3UploadService.deleteFiles(deletableKeys);
                } catch (Exception e) {
                    log.error("게스트 S3 파일 삭제 실패: user={}", guest.getUserEmail(), e);
                }
            }

            tripRepository.deleteAll(trips);
        }

        userRepository.deleteAll(expiredGuests);
        log.info("만료된 게스트 계정 정리 완료 — {}건", expiredGuests.size());
    }
}