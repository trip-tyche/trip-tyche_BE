package com.triptyche.backend.domain.notification.service;

import com.triptyche.backend.domain.notification.dto.NotificationDetailDTO;
import com.triptyche.backend.domain.notification.dto.NotificationResponseDTO;
import com.triptyche.backend.domain.notification.model.Notification;
import com.triptyche.backend.domain.notification.model.NotificationStatus;
import com.triptyche.backend.domain.notification.repository.NotificationRepository;
import com.triptyche.backend.domain.trip.model.Trip;
import com.triptyche.backend.domain.trip.repository.TripRepository;
import com.triptyche.backend.global.common.ResultCode;
import com.triptyche.backend.global.exception.CustomException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

  private final NotificationRepository notificationRepository;
  private final TripRepository tripRepository;


  @Transactional(readOnly = true)
  public List<NotificationResponseDTO> getUnreadNotifications(Long userId) {
    return notificationRepository.findByUserIdAndStatusNot(userId, NotificationStatus.DELETE)
            .stream()
            .map(notification -> new NotificationResponseDTO(
                    notification.getNotificationId(),
                    notification.getReferenceId(),
                    notification.getMessage().name(),
                    notification.getStatus().name(),
                    notification.getSenderNickname(),
                    notification.getCreatedAt().toString()
            ))
            .collect(Collectors.toList());
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
  public NotificationDetailDTO getNotificationDetail(Long notificationId) {
    Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new CustomException(ResultCode.NOTIFICATION_NOT_FOUND));
    Optional<Trip> trip = tripRepository.findById(notification.getReferenceId());
    String tripTitle = trip.map(Trip::getTripTitle).orElse("UNKNOWN_TRIP");

    return new NotificationDetailDTO(
            tripTitle,
            notification.getMessage(),
            notification.getSenderNickname()
    );
  }


  @Transactional
  public void markAsDeleted(List<Long> notificationIds) {
    log.debug("▶▶▶ markAsDeleted 호출, ids = {}", notificationIds);

    List<Notification> notifications = notificationRepository.findAllById(notificationIds);

    notifications.stream()
            .filter(n -> n.getStatus() != NotificationStatus.DELETE)
            .forEach(n -> {
              n.markAsDeleted();
              log.debug("[{}] 알림 상태 변화 = {}", n.getNotificationId(), n.getStatus());
            });
  }

}
