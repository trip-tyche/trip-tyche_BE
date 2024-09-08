package com.fivefeeling.memory.domain.media.repository;

import com.fivefeeling.memory.domain.media.model.MediaFile;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MediaFileRepository extends JpaRepository<MediaFile, Long> {

  List<MediaFile> findByPinPointPinPointId(Long pinPointId);

  // tripId와 pinPointId에 해당하는 MediaFile 조회
  List<MediaFile> findByTripTripIdAndPinPointPinPointId(Long tripId, Long pinPointId);

  // String 형식의 날짜를 Date로 변환하여 조회
  @Query("SELECT m FROM MediaFile m WHERE m.trip.tripId = :tripId AND FUNCTION('DATE', m.recordDate) = :recordDate")
  List<MediaFile> findByTripTripIdAndRecordDate(
      @Param("tripId") Long tripId,
      @Param("recordDate") String recordDate
  );
}
