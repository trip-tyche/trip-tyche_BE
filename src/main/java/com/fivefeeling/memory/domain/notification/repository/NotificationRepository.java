package com.fivefeeling.memory.domain.notification.repository;

import com.fivefeeling.memory.domain.notification.model.Notification;
import com.fivefeeling.memory.domain.notification.model.NotificationStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

  List<Notification> findByUserIdAndStatus(Long userId, NotificationStatus status);

  List<Notification> findByUserId(Long userId);
}
