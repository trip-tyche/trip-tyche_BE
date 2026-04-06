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

        // 전체 게스트의 Trip을 한 번에 조회 (N+1 방지)
        List<Trip> allTrips = tripRepository.findAllByUserIn(expiredGuests);

        // 전체 Trip의 MediaFile을 한 번에 조회 후 S3 삭제 (demo/ 경로 제외)
        if (!allTrips.isEmpty()) {
            List<String> deletableKeys = mediaFileRepository.findAllByTripIn(allTrips).stream()
                    .map(mf -> mf.getMediaKey())
                    .filter(key -> !key.startsWith("demo/"))
                    .toList();

            if (!deletableKeys.isEmpty()) {
                try {
                    s3UploadService.deleteFiles(deletableKeys);
                } catch (Exception e) {
                    log.error("게스트 S3 파일 삭제 실패: {}건", deletableKeys.size(), e);
                }
            }

            tripRepository.deleteAll(allTrips);
        }

        userRepository.deleteAll(expiredGuests);
        log.info("만료된 게스트 계정 정리 완료 — {}건", expiredGuests.size());
    }
}