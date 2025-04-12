package com.fivefeeling.memory.domain.trip.service;

import com.fivefeeling.memory.domain.media.service.MediaProcessingService;
import com.fivefeeling.memory.domain.share.repository.ShareRepository;
import com.fivefeeling.memory.domain.trip.dto.TripCreationResponseDTO;
import com.fivefeeling.memory.domain.trip.dto.TripInfoRequestDTO;
import com.fivefeeling.memory.domain.trip.model.Trip;
import com.fivefeeling.memory.domain.trip.repository.TripRepository;
import com.fivefeeling.memory.domain.trip.validator.TripAccessValidator;
import com.fivefeeling.memory.domain.user.model.User;
import com.fivefeeling.memory.domain.user.repository.UserRepository;
import com.fivefeeling.memory.global.common.ResultCode;
import com.fivefeeling.memory.global.exception.CustomException;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TripManagementService {

  private final TripRepository tripRepository;
  private final UserRepository userRepository;
  private final ShareRepository shareRepository;
  private final MediaProcessingService mediaProcessingService;
  private final TripAccessValidator tripAccessValidator;


  public TripCreationResponseDTO createTripId(String userEmail) {
    User user = userRepository.findByUserEmail(userEmail)
            .orElseThrow(() -> new CustomException(ResultCode.USER_NOT_FOUND));

    Trip trip = Trip.builder()
            .user(user)
            .tripTitle("임시 제목")
            .country("미정")
            .startDate(LocalDate.now())
            .endDate(LocalDate.now())
            .hashtags("")
            .status("DRAFT")
            .build();

    tripRepository.save(trip);
    return new TripCreationResponseDTO(trip.getTripKey());
  }

  @Transactional
  public void finalizeTrip(String userEmail, Long tripId) {
    Trip trip = tripAccessValidator.validateAccessibleTrip(tripId, userEmail);

    // 이미 확정된 경우 예외 처리
    if (!"DRAFT".equals(trip.getStatus())) {
      throw new CustomException(ResultCode.INVALID_TRIP_STATE);
    }

    trip.setStatus("CONFIRMED");
    tripRepository.save(trip);
  }

  // 사용자 여행 정보 저장 및 수정
  @Transactional
  public void updateTrip(String userEmail, Long tripId, TripInfoRequestDTO tripInfoRequestDTO) {
    Trip trip = tripAccessValidator.validateAccessibleTrip(tripId, userEmail);

    trip.setTripTitle(tripInfoRequestDTO.tripTitle());
    trip.setCountry(tripInfoRequestDTO.country());
    trip.setStartDate(tripInfoRequestDTO.startDate());
    trip.setEndDate(tripInfoRequestDTO.endDate());
    trip.setHashtagsFromList(tripInfoRequestDTO.hashtags());

    tripRepository.save(trip);
  }

  // 사용자 여행 정보 삭제
  @Transactional
  public void deleteTrip(String userEmail, Long tripId) {
    Trip trip = tripAccessValidator.validateAccessibleTrip(tripId, userEmail);

    shareRepository.deleteAllByTrip(trip);

    // 미디어 파일 삭제
    mediaProcessingService.deleteMediaFilesByTrip(trip);

    // 여행 삭제
    tripRepository.delete(trip);
  }
}
