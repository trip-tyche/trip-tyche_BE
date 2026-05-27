package com.triptyche.backend.domain.media.model;

import com.triptyche.backend.domain.trip.model.PinPoint;
import com.triptyche.backend.domain.trip.model.Trip;
import com.triptyche.backend.global.common.ResultCode;
import com.triptyche.backend.global.exception.CustomException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "media_file", indexes = {
    @Index(name = "idx_media_file_trip_id", columnList = "trip_id"),
    @Index(name = "idx_media_file_pin_point_id", columnList = "pin_point_id"),
    @Index(name = "idx_media_file_trip_id_record_date", columnList = "trip_id, record_date"),
    // 동일 tripKey로 metadata POST가 반복돼도 같은 fileKey는 한 행만 존재하도록 강제.
    // service-level dedup과 함께 idempotent 등록을 보장한다.
    @Index(name = "uq_media_file_trip_id_media_key", columnList = "trip_id, media_key", unique = true)
})
public class MediaFile {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long mediaFileId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "trip_id", nullable = false)
  private Trip trip;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "pin_point_id", nullable = false)
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

  @Column(length = 255)
  private String tempKey;

  @Column(length = 255)
  private String finalKey;

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private ProcessingStatus processingStatus;

  @Temporal(TemporalType.TIMESTAMP)
  private LocalDateTime processedAt;

  @Column(length = 500)
  private String failureReason;

  public void updateLocation(Double latitude, Double longitude, PinPoint pinPoint) {
    this.latitude = latitude;
    this.longitude = longitude;
    this.pinPoint = pinPoint;
  }

  public void updateRecordDate(LocalDateTime recordDate) {
    this.recordDate = recordDate;
  }

  public void markUploaded() {
    if (processingStatus != null) {
      throw new CustomException(ResultCode.INVALID_MEDIA_STATE);
    }
    this.processingStatus = ProcessingStatus.UPLOADED;
  }

  public void markProcessing() {
    if (processingStatus != ProcessingStatus.UPLOADED) {
      throw new CustomException(ResultCode.INVALID_MEDIA_STATE);
    }
    this.processingStatus = ProcessingStatus.PROCESSING;
  }

  public void markProcessed(LocalDateTime processedAt) {
    if (processingStatus != ProcessingStatus.UPLOADED && processingStatus != ProcessingStatus.PROCESSING) {
      throw new CustomException(ResultCode.INVALID_MEDIA_STATE);
    }
    this.processingStatus = ProcessingStatus.PROCESSED;
    this.processedAt = processedAt;
    this.failureReason = null;
  }

  public void markFailed(String reason) {
    if (processingStatus == ProcessingStatus.PROCESSED) {
      throw new CustomException(ResultCode.INVALID_MEDIA_STATE);
    }
    this.processingStatus = ProcessingStatus.FAILED;
    this.failureReason = reason;
  }

  public void updateMediaLink(String mediaLink) {
    this.mediaLink = mediaLink;
  }
}