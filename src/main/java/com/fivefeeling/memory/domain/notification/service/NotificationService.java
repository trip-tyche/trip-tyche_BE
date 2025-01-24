package com.fivefeeling.memory.domain.notification.service;

import com.fivefeeling.memory.domain.notification.dto.NotificationResponseDTO;
import com.fivefeeling.memory.domain.notification.model.Notification;
import com.fivefeeling.memory.domain.notification.model.Notification.NotificationStatus;
import com.fivefeeling.memory.domain.notification.repository.NotificationRepository;
import com.fivefeeling.memory.global.common.ResultCode;
import com.fivefeeling.memory.global.exception.CustomException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {

  private final NotificationRepository notificationRepository;
  private final RedisTemplate<String, Object> redisTemplate;

  public List<NotificationResponseDTO> getUnreadNotifications(Long userId) {
    List<Notification> notifications = notificationRepository.findByUserIdAndStatus(
            userId, Notification.NotificationStatus.UNREAD
    );

    return notifications.stream()
            .map(notification -> new NotificationResponseDTO(
                    notification.getNotificationId(),
                    notification.getMessage(),
                    notification.getStatus().name(),
                    notification.getCreatedAt()
            ))
            .collect(Collectors.toList());
  }

  public NotificationResponseDTO markAsRead(Long notificationId) {
    Optional<Notification> notificationOpt = notificationRepository.findById(notificationId);
    if (notificationOpt.isEmpty()) {
      throw new CustomException(ResultCode.NOTIFICATION_NOT_FOUND);
    }

    Notification notification = notificationOpt.get();

    if (notification.getStatus() == NotificationStatus.READ) {
      return toDTO(notification);
    }

    if (notification.getStreamMessageId() != null) {
      try {
        redisTemplate.opsForStream().delete(
                "shareRequests",
                RecordId.of(notification.getStreamMessageId())
        );
      } catch (Exception e) {
        throw new RuntimeException("Redis Stream 삭제 중 오류가 발생했습니다." + e);
      }
    }

    notification.markAsRead();

    Notification saved = notificationRepository.save(notification);
    return toDTO(saved);
  }

  private NotificationResponseDTO toDTO(Notification notification) {
    return new NotificationResponseDTO(
            notification.getNotificationId(),
            notification.getMessage(),
            notification.getStatus().toString(),
            notification.getCreatedAt()
    );
  }
}
