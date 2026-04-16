package com.triptyche.backend.domain.media.model;

import com.triptyche.backend.domain.trip.model.PinPoint;
import com.triptyche.backend.domain.trip.model.Trip;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(name = "MediaFile", indexes = {
    @Index(name = "idx_media_file_trip_id", columnList = "tripId"),
    @Index(name = "idx_media_file_pin_point_id", columnList = "pinPointId"),
    @Index(name = "idx_media_file_trip_id_record_date", columnList = "tripId, recordDate")
})
public class MediaFile {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long mediaFileId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tripId", nullable = false)
  private Trip trip;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "pinPointId", nullable = false)
  private PinPoint pinPoint;

  @Column(length = 50)
  private String mediaType;

  @Column(length = 255)
  private String mediaLink;

  @Temporal(TemporalType.TIMESTAMP)
  private LocalDateTime recordDate;

  private Double latitude;

  private Double longitude;

  @Column(nullable = false, length = 255)
  private String mediaKey;

  public void updateLocation(Double latitude, Double longitude, PinPoint pinPoint) {
    this.latitude = latitude;
    this.longitude = longitude;
    this.pinPoint = pinPoint;
  }

  public void updateRecordDate(LocalDateTime recordDate) {
    this.recordDate = recordDate;
  }
}