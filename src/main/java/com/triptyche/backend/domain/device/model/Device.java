package com.triptyche.backend.domain.device.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(
        name = "device",
        uniqueConstraints = @UniqueConstraint(name = "uq_device_token", columnNames = "token"),
        indexes = @Index(name = "idx_device_user_id", columnList = "user_id")
)
public class Device {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long deviceId;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "token", nullable = false, length = 255)
  private String token;

  @Column(name = "platform", nullable = false, length = 20)
  @Enumerated(EnumType.STRING)
  private DevicePlatform platform;

  @Column(name = "app_version", length = 20)
  private String appVersion;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onPersist() {
    LocalDateTime now = LocalDateTime.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = LocalDateTime.now();
  }

  public void updateRegistration(Long userId, DevicePlatform platform, String appVersion) {
    this.userId = userId;
    this.platform = platform;
    this.appVersion = appVersion;
  }
}
