package com.triptyche.backend.domain.notification.service;

import com.triptyche.backend.domain.notification.dto.NotificationDetailResponse;
import com.triptyche.backend.domain.notification.dto.NotificationResponse;
import com.triptyche.backend.domain.notification.model.Notification;
import com.triptyche.backend.domain.notification.model.NotificationStatus;
import com.triptyche.backend.domain.notification.repository.NotificationRepository;
import com.triptyche.backend.domain.trip.service.TripQueryService;
import com.triptyche.backend.global.common.ResultCode;
import com.triptyche.backend.global.exception.CustomException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

  private final NotificationRepository notificationRepository;
  private final TripQueryService tripQueryService;


  @Transactional(readOnly = true)
  public List<NotificationResponse> getActiveNotifications(Long userId) {
    return notificationRepository.findByUserIdAndStatusNot(userId, NotificationStatus.DELETE)
            .stream()
            .map(notification -> new NotificationResponse(
                    notification.getNotificationId(),
                    notification.getReferenceId(),
                    notification.getMessage().name(),
                    notification.getStatus().name(),
                    notification.getSenderNickname(),
                    notification.getCreatedAt().toString()
            ))
            .toList();
  }

  @Transactional
  public void markAsRead(Long notificationId) {
    Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new CustomException(ResultCode.NOTIFICATION_NOT_FOUND));

    if (notification.getStatus() == NotificationStatus.READ) {
      return;
    }

    notification.markAsRead();
  }

  @Transactional(readOnly = true)
  public NotificationDetailResponse getNotificationDetail(Long notificationId) {
    Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new CustomException(ResultCode.NOTIFICATION_NOT_FOUND));
    String tripTitle = tripQueryService.getTripTitleById(notification.getReferenceId());

    return new NotificationDetailResponse(
            tripTitle,
            notification.getMessage(),
            notification.getSenderNickname()
    );
  }


  @Transactional
  public void markAsDeleted(List<Long> notificationIds) {
    List<Notification> notifications = notificationRepository.findAllById(notificationIds);

    notifications.stream()
            .filter(n -> n.getStatus() != NotificationStatus.DELETE)
            .forEach(n -> {
              n.markAsDeleted();
              log.debug("[{}] 알림 상태 변화 → {}", n.getNotificationId(), n.getStatus());
            });
  }

}
