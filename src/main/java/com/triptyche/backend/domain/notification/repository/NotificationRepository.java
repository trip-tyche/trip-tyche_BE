package com.triptyche.backend.domain.notification.repository;

import com.triptyche.backend.domain.notification.model.Notification;
import com.triptyche.backend.domain.notification.model.NotificationStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

  List<Notification> findByUserId(Long userId);

  long countByUserIdAndStatus(Long userId, NotificationStatus status);
}
