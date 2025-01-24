package com.fivefeeling.memory.domain.notification.service;

import com.fivefeeling.memory.domain.notification.dto.NotificationResponseDTO;
import com.fivefeeling.memory.domain.notification.model.Notification;
import com.fivefeeling.memory.domain.notification.repository.NotificationRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {

  private final NotificationRepository notificationRepository;

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
}
