package com.fivefeeling.memory.domain.share.model;

import com.fivefeeling.memory.domain.trip.model.Trip;
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
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "share")
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

  @Setter
  @Column(name = "share_status", nullable = false)
  @Enumerated(EnumType.STRING)
  private ShareStatus shareStatus;

  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

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
