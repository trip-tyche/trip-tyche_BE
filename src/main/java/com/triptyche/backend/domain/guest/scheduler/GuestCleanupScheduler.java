package com.triptyche.backend.domain.guest.scheduler;

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

@Slf4j
@Component
@RequiredArgsConstructor
public class GuestCleanupScheduler {

    private final UserRepository userRepository;
    private final GuestCleanupExecutor guestCleanupExecutor;
    private final S3UploadService s3UploadService;

    private static final int GUEST_SESSION_HOURS = 4;

    @Scheduled(cron = "0 0 * * * *")
    public void cleanupExpiredGuests() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(GUEST_SESSION_HOURS);
        List<User> expiredGuests = userRepository.findByRoleAndCreatedAtBefore(UserRole.GUEST, threshold);

        if (expiredGuests.isEmpty()) {
            log.debug("만료된 게스트 계정 없음 — 정리 생략");
            return;
        }

        List<String> s3Keys = guestCleanupExecutor.deleteExpiredGuests(expiredGuests);
        deleteFromStorage(s3Keys);
    }

    private void deleteFromStorage(List<String> s3Keys) {
        if (s3Keys.isEmpty()) return;
        try {
            s3UploadService.deleteFiles(s3Keys);
            log.info("게스트 S3 파일 정리 완료 — {}건", s3Keys.size());
        } catch (Exception e) {
            log.error("게스트 S3 파일 정리 실패 — {}건, 수동 확인 필요", s3Keys.size(), e);
        }
    }
}
