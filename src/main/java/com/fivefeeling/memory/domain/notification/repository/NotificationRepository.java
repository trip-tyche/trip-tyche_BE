package com.fivefeeling.memory.domain.notification.repository;

import com.fivefeeling.memory.domain.notification.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

}
