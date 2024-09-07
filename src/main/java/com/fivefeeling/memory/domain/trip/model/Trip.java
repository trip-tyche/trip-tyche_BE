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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.time.LocalDate;
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
  @JoinColumn(name = "userId", nullable = false)
  private User user;

  @Column(name = "tripTitle", nullable = false, length = 255)
  private String tripTitle;

  @Column(name = "country", nullable = false, length = 255)
  private String country;

  @Column(name = "startDate", nullable = false)
  private LocalDate startDate;

  @Column(name = "endDate", nullable = false)
  private LocalDate endDate;

  @Column(name = "hashtags", length = 255)
  private String hashtags;

  @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<PinPoint> pinPoints;

  // 해시태그 리스트로 처리
  public void setHashtagsFromList(List<String> hashtags) {
    this.hashtags = String.join(",", hashtags);
  }

  public List<String> getHashtagsAsList() {
    return List.of(this.hashtags.split(","));
  }
}
