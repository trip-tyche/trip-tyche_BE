package com.fivefeeling.memory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "PinPoint")
public class PinPoint {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long pinPointId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tripId", nullable = false)
  private Trip trip;

  @Column(name = "latitude")
  private Float latitude;

  @Column(name = "longitude")
  private Float longitude;

  @Column(name = "pinPointImageDate")
  private Date pinPointImageDate;

  @Column(name = "pinPointImageLink", length = 255)
  private String pinPointImageLink;

}