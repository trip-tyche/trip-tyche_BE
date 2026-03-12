package com.triptyche.backend.domain.trip.model;


import com.triptyche.backend.domain.pinpoint.model.PinPoint;
import com.triptyche.backend.domain.user.model.User;
import com.triptyche.backend.global.common.ResultCode;
import com.triptyche.backend.global.exception.CustomException;
import com.triptyche.backend.global.util.TripKeyGenerator;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.SQLRestriction;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString(exclude = {"user", "pinPoints"})
@Builder
@SQLRestriction("deleted_at IS NULL")
@Entity
public class Trip {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long tripId;

  @Column(name = "trip_key", unique = true, nullable = false)
  private String tripKey;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "trip_title")
  private String tripTitle;

  @Column(name = "country")
  private String country;

  @Column(name = "start_date")
  private LocalDate startDate;

  @Column(name = "end_date")
  private LocalDate endDate;

  @Column(name = "hashtags", length = 255)
  private String hashtags;

  @Enumerated(EnumType.STRING)
  @Column(name = "status")
  private TripStatus status;

  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @Builder.Default
  @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<PinPoint> pinPoints = new ArrayList<>();

  // 해시태그 리스트로 처리
  public void setHashtagsFromList(List<String> hashtags) {
    this.hashtags = String.join(",", hashtags);
  }

  public List<String> getHashtagsAsList() {
    return List.of(this.hashtags.split(","));
  }

  public void updateInfo(String tripTitle, String country,
                         LocalDate startDate, LocalDate endDate,
                         List<String> hashtags) {
    this.tripTitle = tripTitle;
    this.country = country;
    this.startDate = startDate;
    this.endDate = endDate;
    this.hashtags = String.join(",", hashtags);
  }

  public void markImagesUploaded() {
    if (this.status != TripStatus.DRAFT) {
      throw new CustomException(ResultCode.INVALID_TRIP_STATE);
    }
    this.status = TripStatus.IMAGES_UPLOADED;
  }

  public void confirmTrip() {
    if (this.status != TripStatus.IMAGES_UPLOADED) {
      throw new CustomException(ResultCode.INVALID_TRIP_STATE);
    }
    this.status = TripStatus.CONFIRMED;
  }

  public boolean isOwnedBy(Long userId) {
    return this.user.getUserId().equals(userId);
  }

  public boolean isConfirmed() {
    return this.status == TripStatus.CONFIRMED;
  }

  public void softDelete() {
    this.deletedAt = LocalDateTime.now();
  }

  public boolean isDeleted() {
    return this.deletedAt != null;
  }

  @PrePersist
  public void prePersist() {
    if (this.tripKey == null || this.tripKey.isEmpty()) {
      this.tripKey = TripKeyGenerator.generateKey();
    }
    this.createdAt = LocalDateTime.now();
  }
}
