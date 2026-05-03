package com.triptyche.backend.domain.guest.service;

import com.triptyche.backend.domain.share.dto.ShareCreateRequest;
import com.triptyche.backend.domain.share.service.ShareService;
import com.triptyche.backend.domain.trip.model.Trip;
import com.triptyche.backend.domain.trip.repository.TripRepository;
import com.triptyche.backend.domain.user.model.User;
import com.triptyche.backend.domain.user.repository.UserRepository;
import com.triptyche.backend.global.common.ResultCode;
import com.triptyche.backend.global.config.GuestProperties;
import com.triptyche.backend.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuestShareTriggerService {

    private final ShareService shareService;
    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final GuestProperties guestProperties;

    @Async
    public void triggerIfNeeded(Long guestUserId) {
        User templateUser = userRepository.findByUserEmail(guestProperties.templateEmail()).orElse(null);
        if (templateUser == null) {
            log.warn("게스트 공유 트리거: 템플릿 계정을 찾을 수 없음 — {}", guestProperties.templateEmail());
            return;
        }

        Trip templateTrip = tripRepository.findAllByUser(templateUser).stream()
                .filter(t -> guestProperties.shareTargetTripTitle().equals(t.getTripTitle()))
                .findFirst()
                .orElse(null);
        if (templateTrip == null) {
            log.warn("게스트 공유 트리거: 공유 대상 트립을 찾을 수 없음 — {}", guestProperties.shareTargetTripTitle());
            return;
        }

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        try {
            shareService.createShare(new ShareCreateRequest(templateTrip.getTripKey(), guestUserId), templateUser);
            log.info("게스트 공유 신청 완료: guestUserId={}, tripKey={}", guestUserId, templateTrip.getTripKey());
        } catch (CustomException e) {
            if (e.getResultCode() != ResultCode.SHARE_ALREADY_EXIST
                    && e.getResultCode() != ResultCode.DUPLICATE_DATA_CONFLICT) {
                log.error("게스트 공유 신청 실패: guestUserId={}, code={}", guestUserId, e.getResultCode());
            }
        } catch (Exception e) {
            log.error("게스트 공유 신청 실패: guestUserId={}, error={}", guestUserId, e.getMessage());
        }
    }
}
