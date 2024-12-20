package com.fivefeeling.memory.domain.pinpoint.repository;

import com.fivefeeling.memory.domain.media.model.MediaFileResponseDTO;
import com.fivefeeling.memory.domain.pinpoint.model.PinPoint;
import com.fivefeeling.memory.domain.pinpoint.model.PinPointResponseDTO;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PinPointRepository extends JpaRepository<PinPoint, Long> {

  @EntityGraph(attributePaths = {"mediaFiles"})
  List<PinPoint> findByTripTripId(Long tripId);

  @Query("""
      SELECT new com.fivefeeling.memory.domain.pinpoint.model.PinPointResponseDTO(
          p.pinPointId,
          CAST(COALESCE(mf.latitude, 0.0) AS double),
          CAST(COALESCE(mf.longitude, 0.0) AS double),
          mf.recordDate,
          mf.mediaLink
      )
      FROM PinPoint p
      JOIN p.mediaFiles mf
      WHERE p.trip.tripId = :tripId
        AND mf.recordDate = (
          SELECT MIN(mf2.recordDate)
          FROM MediaFile mf2
          WHERE mf2.pinPoint = p
        )
        AND mf.mediaFileId = (
          SELECT MIN(mf3.mediaFileId)
          FROM MediaFile mf3
          WHERE mf3.pinPoint = p
            AND mf3.recordDate = mf.recordDate
        )
      ORDER BY mf.recordDate ASC
      """)
  List<PinPointResponseDTO> findEarliestSingleMediaFileForEachPinPointByTripId(@Param("tripId") Long tripId);

  @Query("""
          SELECT new com.fivefeeling.memory.domain.media.model.MediaFileResponseDTO(
              mf.mediaFileId,
              mf.mediaLink,
              mf.mediaType,
              mf.recordDate,
              mf.latitude,
              mf.longitude,
              null
          )
          FROM MediaFile mf
          WHERE mf.pinPoint.trip.tripId = :tripId
      """)
  List<MediaFileResponseDTO> findMediaFilesByTripId(@Param("tripId") Long tripId);
}
