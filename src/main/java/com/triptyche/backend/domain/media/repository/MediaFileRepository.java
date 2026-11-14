package com.triptyche.backend.domain.media.repository;

import com.triptyche.backend.domain.media.dto.MediaFileSummary;
import com.triptyche.backend.domain.media.dto.MediaFileDetailResponse;
import com.triptyche.backend.domain.media.dto.TripCoverRow;
import com.triptyche.backend.domain.media.dto.TripPhotoCountRow;
import com.triptyche.backend.domain.media.model.MediaFile;
import com.triptyche.backend.domain.trip.model.Trip;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface MediaFileRepository extends JpaRepository<MediaFile, Long> {

  @Query("""
              SELECT new com.triptyche.backend.domain.media.dto.MediaFileDetailResponse(
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
  List<MediaFileDetailResponse> findByTripTripIdAndPinPointPinPointId(
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
              SELECT new com.triptyche.backend.domain.media.dto.MediaFileSummary(
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
  List<MediaFileSummary> findByTripTripIdAndRecordDate(
          @Param("tripId") Long tripId,
          @Param("startOfDay") LocalDateTime startOfDay,
          @Param("endOfDay") LocalDateTime endOfDay,
          @Param("defaultDate") LocalDateTime defaultDate

  );

  List<MediaFile> findByTripTripId(Long tripId);

  /**
   * 여행별 표지 사진 1장. 처리 완료(media_link IS NOT NULL)된 사진 중
   * 날짜 없음(NULL·1980 기본값)을 뒤로 보내고 record_date, media_file_id 순으로 첫 행을 고른다.
   */
  @Query(value = """
          SELECT t.trip_id AS tripId, t.media_link AS mediaLink
          FROM (
            SELECT m.trip_id, m.media_link,
                   ROW_NUMBER() OVER (
                     PARTITION BY m.trip_id
                     ORDER BY CASE WHEN m.record_date IS NULL OR m.record_date = :defaultDate THEN 1 ELSE 0 END,
                              m.record_date,
                              m.media_file_id
                   ) AS rn
            FROM media_file m
            WHERE m.trip_id IN (:tripIds)
              AND m.media_link IS NOT NULL
          ) t
          WHERE t.rn = 1
          """, nativeQuery = true)
  List<TripCoverRow> findCoversByTripIds(
          @Param("tripIds") Collection<Long> tripIds,
          @Param("defaultDate") LocalDateTime defaultDate
  );

  /**
   * 여행별 media_file 전체 행 수(처리 중 사진 포함).
   */
  @Query("""
          SELECT m.trip.tripId AS tripId, COUNT(m) AS photoCount
          FROM MediaFile m
          WHERE m.trip.tripId IN :tripIds
          GROUP BY m.trip.tripId
          """)
  List<TripPhotoCountRow> countByTripIds(@Param("tripIds") Collection<Long> tripIds);

  @Query("""
          SELECT m FROM MediaFile m
          LEFT JOIN FETCH m.pinPoint
          WHERE m.trip.tripId = :tripId
          """)
  List<MediaFile> findByTripTripIdWithPinPoint(@Param("tripId") Long tripId);

  /**
   * 사용자가 같은 tripKey로 metadata POST를 반복했을 때 중복 INSERT를 막기 위한 사전 dedup 용도.
   */
  @Query("""
          SELECT m.mediaKey
          FROM MediaFile m
          WHERE m.trip.tripId = :tripId
            AND m.mediaKey IN :mediaKeys
          """)
  List<String> findExistingMediaKeys(
          @Param("tripId") Long tripId,
          @Param("mediaKeys") Collection<String> mediaKeys
  );

  @Query("""
          SELECT DISTINCT CAST(m.recordDate AS localdate)
          FROM MediaFile m
          WHERE m.trip.tripId = :tripId
            AND m.recordDate <> :defaultDate
          ORDER BY CAST(m.recordDate AS localdate) ASC
          """)
  List<LocalDate> findDistinctRecordDatesByTripId(
          @Param("tripId") Long tripId,
          @Param("defaultDate") LocalDateTime defaultDate
  );

  List<MediaFile> findAllByTripIn(List<Trip> trips);

  @Transactional
  @Modifying(clearAutomatically = true)
  @Query("DELETE FROM MediaFile m WHERE m.trip IN :trips")
  void deleteAllByTripIn(@Param("trips") List<Trip> trips);

  @Query("""
          SELECT m FROM MediaFile m
          WHERE m.processingStatus IS NULL
            AND m.tempKey IS NOT NULL
            AND m.createdAt < :threshold
          """)
  List<MediaFile> findOrphans(@Param("threshold") LocalDateTime threshold);
}
