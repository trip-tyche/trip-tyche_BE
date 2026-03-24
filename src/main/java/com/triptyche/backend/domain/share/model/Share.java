package com.triptyche.backend.domain.share.model;

import com.triptyche.backend.domain.trip.model.Trip;
import com.triptyche.backend.global.common.ResultCode;
import com.triptyche.backend.global.exception.CustomException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "share",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_share_trip_recipient",
        columnNames = {"trip_id", "recipient_id"}
    )
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Share {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long shareId;

  @ManyToOne
  @JoinColumn(name = "trip_id", nullable = false)
  private Trip trip;

  @Column(name = "recipient_id", nullable = false)
  private Long recipientId;

  @Column(name = "share_status", nullable = false)
  @Enumerated(EnumType.STRING)
  private ShareStatus shareStatus;

  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  public void updateStatus(ShareStatus newStatus) {
    if (this.shareStatus != ShareStatus.PENDING) {
      throw new CustomException(ResultCode.INVALID_SHARE_STATUS_TRANSITION);
    }
    this.shareStatus = newStatus;
  }

  @PrePersist
  protected void onPersist() {
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
    if (this.shareStatus == null) {
      this.shareStatus = ShareStatus.PENDING;
    }
  }

  @PreUpdate
  protected void onPreUpdate() {
    this.updatedAt = LocalDateTime.now();
  }

}
