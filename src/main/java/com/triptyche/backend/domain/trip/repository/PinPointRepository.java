package com.triptyche.backend.domain.trip.repository;

import com.triptyche.backend.domain.media.dto.MediaFileResponse;
import com.triptyche.backend.domain.trip.dto.PinPointResponse;
import com.triptyche.backend.domain.trip.model.PinPoint;
import com.triptyche.backend.domain.trip.model.Trip;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface PinPointRepository extends JpaRepository<PinPoint, Long> {

  @EntityGraph(attributePaths = {"mediaFiles"})
  List<PinPoint> findByTripTripId(Long tripId);

  List<PinPoint> findAllByTripTripId(Long tripId);

  @Query("""
          SELECT new com.triptyche.backend.domain.trip.dto.PinPointResponse(
              p.pinPointId,
              CAST(COALESCE(mf.latitude, 0.0) AS double),
              CAST(COALESCE(mf.longitude, 0.0) AS double),
              mf.recordDate,
              mf.mediaLink
          )
          FROM PinPoint p
          JOIN p.mediaFiles mf
          WHERE p.trip.tripId = :tripId
            AND NOT (mf.latitude = 0.0 AND mf.longitude = 0.0)
            AND mf.recordDate <> :defaultDate
            AND mf.recordDate = (
              SELECT MIN(mf2.recordDate)
              FROM MediaFile mf2
              WHERE mf2.pinPoint = p
                AND NOT (mf2.latitude = 0 AND mf2.longitude = 0)
                AND mf2.recordDate <> :defaultDate
            )
            AND mf.mediaFileId = (
              SELECT MIN(mf3.mediaFileId)
              FROM MediaFile mf3
              WHERE mf3.pinPoint = p
                AND mf3.recordDate = mf.recordDate
                AND NOT (mf3.latitude = 0 AND mf3.longitude = 0)
                AND mf3.recordDate <> :defaultDate
            )
          ORDER BY mf.recordDate ASC
          """)
  List<PinPointResponse> findEarliestSingleMediaFileForEachPinPointByTripId(
          @Param("tripId") Long tripId,
          @Param("defaultDate") LocalDateTime defaultDate);

  @Transactional
  @Modifying(clearAutomatically = true)
  @Query("DELETE FROM PinPoint p WHERE p.trip IN :trips")
  void deleteAllByTripIn(@Param("trips") List<Trip> trips);

  @Query("""
              SELECT new com.triptyche.backend.domain.media.dto.MediaFileResponse(
                  mf.mediaFileId,
                  mf.mediaLink,
                  mf.recordDate,
                  mf.latitude,
                  mf.longitude
              )
              FROM MediaFile mf
              WHERE mf.pinPoint.trip.tripId = :tripId
                AND NOT (mf.latitude = 0 AND mf.longitude = 0)
                AND mf.recordDate <> :defaultDate
              ORDER BY mf.recordDate ASC
          """)
  List<MediaFileResponse> findMediaFilesByTripId(
          @Param("tripId") Long tripId,
          @Param("defaultDate") LocalDateTime defaultDate);
}
