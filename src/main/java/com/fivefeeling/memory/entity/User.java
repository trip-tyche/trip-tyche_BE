package com.fivefeeling.memory.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Getter
@DynamicUpdate
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "userId")
  private Long userId;

  @Column(name = "userName", nullable = false, length = 255)
  private String userName;

  @Column(name = "userNickName", length = 255)
  private String userNickName;

  @Column(name = "userEmail", nullable = false, length = 255, unique = true)
  private String userEmail;

  @Column(name = "provider", nullable = false)
  private String provider;

  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Trip> trips;

  public User updateUser(String userName, String userEmail) {
    this.userName = userName;
    this.userEmail = userEmail;
    return this;
  }

}
