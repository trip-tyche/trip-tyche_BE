package com.triptyche.backend.domain.user.scheduler;

import com.triptyche.backend.domain.share.dto.ShareCreateRequest;
import com.triptyche.backend.domain.share.service.ShareService;
import com.triptyche.backend.domain.trip.model.Trip;
import com.triptyche.backend.domain.trip.repository.TripRepository;
import com.triptyche.backend.domain.user.model.User;
import com.triptyche.backend.domain.user.repository.UserRepository;
import com.triptyche.backend.global.config.GuestProperties;
import com.triptyche.backend.global.redis.GuestShareQueueRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GuestShareScheduler {

    private final GuestShareQueueRepository guestShareQueueRepository;
    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final ShareService shareService;
    private final GuestProperties guestProperties;

    @Scheduled(cron = "${triptyche.guest.share-poll-cron}")
    public void sendShareToReadyGuests() {
        List<Long> dueIds = guestShareQueueRepository.pollDueIds();
        if (dueIds.isEmpty()) {
            return;
        }

        User templateUser = userRepository.findByUserEmail(guestProperties.templateEmail()).orElse(null);
        if (templateUser == null) {
            log.warn("게스트 공유 스케줄러: 템플릿 계정을 찾을 수 없음 — {}", guestProperties.templateEmail());
            return;
        }

        List<Trip> templateTrips = tripRepository.findAllByUser(templateUser);
        if (templateTrips.isEmpty()) {
            log.warn("게스트 공유 스케줄러: 템플릿 여행 데이터 없음");
            return;
        }

        Trip targetTrip;
        try {
            targetTrip = templateTrips.stream()
                    .filter(t -> guestProperties.shareTargetTripTitle().equals(t.getTripTitle()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "공유 대상 트립을 찾을 수 없습니다: " + guestProperties.shareTargetTripTitle()));
        } catch (IllegalStateException e) {
            log.error("게스트 공유 스케줄러: {}", e.getMessage());
            return;
        }

        for (Long guestUserId : dueIds) {
            try {
                ShareCreateRequest request = new ShareCreateRequest(targetTrip.getTripKey(), guestUserId);
                shareService.createShare(request, templateUser);
                log.info("게스트 공유 신청 완료: guestUserId={}, tripKey={}", guestUserId, targetTrip.getTripKey());
            } catch (Exception e) {
                log.error("게스트 공유 신청 실패: guestUserId={}, error={}", guestUserId, e.getMessage());
            }
        }
    }
}