package com.fivefeeling.memory.domain.notification.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "notification")
public class Notification {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long notificationId;

  @Column(name = "userId", nullable = false)
  private Long userId;

  @Column(name = "message", nullable = false)
  @Enumerated(EnumType.STRING)
  private NotificationType message;

  @Column(name = "status", nullable = false)
  @Enumerated(EnumType.STRING)
  private NotificationStatus status;

  @Setter
  @Column(name = "streamMessageId")
  private String streamMessageId;

  @Column(name = "createdAt", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "shareId")
  private Long shareId;

  @PrePersist
  protected void onPersist() {
    this.createdAt = LocalDateTime.now();
  }

  public void markAsRead() {
    this.status = NotificationStatus.READ;
  }
}
