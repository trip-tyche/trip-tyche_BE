package com.triptyche.backend.domain.media.model;

import com.triptyche.backend.domain.pinpoint.model.PinPoint;
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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
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

  @Column(name = "mediaType", length = 50)
  private String mediaType;

  @Column(name = "mediaLink", length = 255)
  private String mediaLink;

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "recordDate")
  private LocalDateTime recordDate;

  @Column(name = "latitude")
  private Double latitude;

  @Column(name = "longitude")
  private Double longitude;

  @Column(name = "mediaKey", nullable = false, length = 255)
  private String mediaKey;
}
