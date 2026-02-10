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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class NotificationService {

  private final NotificationRepository notificationRepository;
  private final TripRepository tripRepository;
  private final RedisTemplate<String, Object> redisTemplate;


  public List<NotificationResponseDTO> getUnreadNotifications(Long userId) {
    List<Notification> notifications = notificationRepository.findByUserId(userId);

    return notifications.stream()
            .filter(notification -> notification.getStatus() != NotificationStatus.DELETE)
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

  public void markAsRead(Long notificationId) {
    Optional<Notification> notificationOpt = notificationRepository.findById(notificationId);
    if (notificationOpt.isEmpty()) {
      throw new CustomException(ResultCode.NOTIFICATION_NOT_FOUND);
    }

    Notification notification = notificationOpt.get();

    if (notification.getStatus() == NotificationStatus.READ) {
      toDTO(notification);
      return;
    }
    notification.markAsRead();
    Notification saved = notificationRepository.save(notification);
  }

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


  public void markAsDeleted(List<Long> notificationIds) {
    log.debug("▶▶▶ markAsDeleted 호출, ids = {}", notificationIds);
    notificationIds.forEach(id -> {
      Notification notification = notificationRepository.findById(id)
              .orElseThrow(() -> new CustomException(ResultCode.NOTIFICATION_NOT_FOUND));

      // 이미 DELETE 상태면 건너뜁니다.
      if (notification.getStatus() == NotificationStatus.DELETE) {
        return;
      }
      // 엔티티 상태를 DELETE로 변경하고 저장
      notification.markAsDeleted();
      log.debug("[{}] 알림 상태 변화 = {}", id, notification.getStatus());
    });
  }

  private NotificationResponseDTO toDTO(Notification notification) {
    return new NotificationResponseDTO(
            notification.getNotificationId(),
            notification.getReferenceId(),
            notification.getMessage().toString(),
            notification.getStatus().toString(),
            notification.getSenderNickname(),
            notification.getCreatedAt().toString()
    );
  }
}
