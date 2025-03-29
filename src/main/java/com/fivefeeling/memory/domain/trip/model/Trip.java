package com.fivefeeling.memory.domain.trip.model;


import com.fivefeeling.memory.domain.pinpoint.model.PinPoint;
import com.fivefeeling.memory.domain.user.model.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trip {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long tripId;

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

  @Column(name = "status")
  private String status;

  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<PinPoint> pinPoints;

  // 해시태그 리스트로 처리
  public void setHashtagsFromList(List<String> hashtags) {
    this.hashtags = String.join(",", hashtags);
  }

  public List<String> getHashtagsAsList() {
    return List.of(this.hashtags.split(","));
  }

  @ManyToMany
  @JoinTable(
          name = "trip_shared_users",
          joinColumns = @JoinColumn(name = "trip_id"),
          inverseJoinColumns = @JoinColumn(name = "user_id")
  )
  private List<User> sharedUsers = new ArrayList<>();

  @PrePersist
  public void prePersist() {
    createdAt = LocalDateTime.now();
  }

  public void addSharedUser(User user) {
    if (!this.sharedUsers.contains(user)) {
      this.sharedUsers.add(user);
    }
  }
}
