package com.fivefeeling.memory.repository;

import com.fivefeeling.memory.entity.MediaFile;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MediaFileRepository extends JpaRepository<MediaFile, Long> {

  List<MediaFile> findByTripTripId(Long tripId);

  List<MediaFile> findByPinPointPinPointId(Long pinPointId);

  // tripId와 pinPointId에 해당하는 MediaFile 조회
  List<MediaFile> findByTripTripIdAndPinPointPinPointId(Long tripId, Long pinPointId);

}
