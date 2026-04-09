package com.triptyche.backend.domain.notification.repository;

import com.triptyche.backend.domain.notification.model.Notification;
import com.triptyche.backend.domain.notification.model.NotificationStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

  List<Notification> findByUserIdAndStatusNot(Long userId, NotificationStatus status);

  long countByUserIdAndStatus(Long userId, NotificationStatus status);

  @Transactional
  @Modifying(clearAutomatically = true)
  @Query("DELETE FROM Notification n WHERE n.userId IN :userIds")
  void deleteAllByUserIdIn(@Param("userIds") List<Long> userIds);
}
