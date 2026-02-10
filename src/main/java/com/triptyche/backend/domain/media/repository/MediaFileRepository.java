package com.triptyche.backend.domain.media.repository;

import com.triptyche.backend.domain.media.dto.MediaFilesByDate;
import com.triptyche.backend.domain.media.dto.PinPointMediaFilesResponseDTO;
import com.triptyche.backend.domain.media.model.MediaFile;
import com.triptyche.backend.domain.trip.model.Trip;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MediaFileRepository extends JpaRepository<MediaFile, Long> {

  /**
   * 특정 tripId, pinPointId 에 대해
   * – latitude/longitude = (0,0)인 레코드 제외
   * – recordDate = defaultDate인 레코드 제외
   */
  @Query("""
              SELECT new com.triptyche.backend.domain.media.dto.PinPointMediaFilesResponseDTO(
                  m.mediaFileId,
                  m.mediaLink,
                  m.recordDate,
                  m.latitude,
                  m.longitude
              )
              FROM MediaFile m
              WHERE m.pinPoint.trip.tripId   = :tripId
                AND m.pinPoint.pinPointId     = :pinPointId
                AND NOT (m.latitude = 0 AND m.longitude = 0)
                AND m.recordDate <> :defaultDate
              ORDER BY m.recordDate ASC
          """)
  List<PinPointMediaFilesResponseDTO> findByTripTripIdAndPinPointPinPointId(
          @Param("tripId") Long tripId,
          @Param("pinPointId") Long pinPointId,
          @Param("defaultDate") LocalDateTime defaultDate
  );

  /**
   * tripId에 속한 미디어 중,
   * – recordDate가 startOfDay~endOfDay 사이
   * – latitude/longitude = (0,0)인 경우 제외
   * – recordDate = 1980-01-01T00:00:00인 경우 제외
   */
  @Query("""
              SELECT new com.triptyche.backend.domain.media.dto.MediaFilesByDate(
                m.mediaFileId,
                m.mediaLink,
                m.recordDate,
                m.latitude,
                m.longitude
              )
              FROM MediaFile m
              WHERE m.trip.tripId = :tripId
                AND m.recordDate BETWEEN :startOfDay AND :endOfDay
                AND NOT (m.latitude = 0 AND m.longitude = 0)
                AND m.recordDate <> :defaultDate
              ORDER BY m.recordDate ASC
          """)
  List<MediaFilesByDate> findByTripTripIdAndRecordDate(
          @Param("tripId") Long tripId,
          @Param("startOfDay") LocalDateTime startOfDay,
          @Param("endOfDay") LocalDateTime endOfDay,
          @Param("defaultDate") LocalDateTime defaultDate

  );

  List<MediaFile> findAllByTrip(Trip trip);

  List<MediaFile> findByTripTripId(Long tripId);

}
