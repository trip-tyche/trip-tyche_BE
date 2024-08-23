package com.fivefeeling.memory.entity;


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
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import java.util.Date;
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

  @Temporal(TemporalType.DATE)
  @Column(name = "startDate", nullable = false)
  private Date startDate;

  @Temporal(TemporalType.DATE)
  @Column(name = "endDate", nullable = false)
  private Date endDate;

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
