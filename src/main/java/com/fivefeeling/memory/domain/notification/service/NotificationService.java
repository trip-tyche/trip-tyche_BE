package com.fivefeeling.memory.domain.notification.service;

import com.fivefeeling.memory.domain.notification.dto.NotificationResponseDTO;
import com.fivefeeling.memory.domain.notification.model.Notification;
import com.fivefeeling.memory.domain.notification.model.NotificationStatus;
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
  }


  public void markAsDeleted(List<Long> notificationIds) {
    notificationIds.forEach(id -> {
      Notification notification = notificationRepository.findById(id)
              .orElseThrow(() -> new CustomException(ResultCode.NOTIFICATION_NOT_FOUND));

      // 이미 DELETE 상태면 건너뜁니다.
      if (notification.getStatus() == NotificationStatus.DELETE) {
        return;
      }

      // Redis Stream에 발행된 메시지도 함께 삭제
      if (notification.getStreamMessageId() != null) {
        try {
          redisTemplate.opsForStream().delete(
                  "shareRequests",
                  RecordId.of(notification.getStreamMessageId())
          );
        } catch (Exception e) {
          throw new RuntimeException("Redis Stream 삭제 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
      }

      // 엔티티 상태를 DELETE로 변경하고 저장
      notification.markAsDeleted();
      notificationRepository.save(notification);
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
