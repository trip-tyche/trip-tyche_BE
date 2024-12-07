package com.fivefeeling.memory.domain.trip.service;

import static com.fivefeeling.memory.global.util.DateFormatter.formatLocalDateToString;

import com.fivefeeling.memory.domain.media.service.MediaProcessingService;
import com.fivefeeling.memory.domain.trip.model.Trip;
import com.fivefeeling.memory.domain.trip.model.TripInfoRequestDTO;
import com.fivefeeling.memory.domain.trip.model.TripInfoResponseDTO;
import com.fivefeeling.memory.domain.trip.repository.TripRepository;
import com.fivefeeling.memory.domain.user.model.User;
import com.fivefeeling.memory.domain.user.repository.UserRepository;
import com.fivefeeling.memory.global.common.ResultCode;
import com.fivefeeling.memory.global.exception.CustomException;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TripManagementService {

  private final TripRepository tripRepository;
  private final UserRepository userRepository;
  private final MediaProcessingService mediaProcessingService;


  public Long createTripId(String userEmail) {
    User user = userRepository.findByUserEmail(userEmail)
        .orElseThrow(() -> new CustomException(ResultCode.USER_NOT_FOUND));

    Trip trip = Trip.builder()
        .user(user)
        .tripTitle("N/A")
        .country("N/A")
        .startDate(LocalDate.now())
        .endDate(LocalDate.now())
        .hashtags("")
        .build();
    tripRepository.save(trip);
    return trip.getTripId();
  }

  // 사용자 여행 정보 저장 및 수정
  @Transactional
  public TripInfoResponseDTO updateTrip(String userEmail, Long tripId, TripInfoRequestDTO tripInfoRequestDTO) {
    userRepository.findByUserEmail(userEmail)
        .orElseThrow(() -> new CustomException(ResultCode.USER_NOT_FOUND));

    Trip trip = tripRepository.findById(tripId)
        .orElseThrow(() -> new CustomException(ResultCode.TRIP_NOT_FOUND));

    trip.setTripTitle(tripInfoRequestDTO.tripTitle());
    trip.setCountry(tripInfoRequestDTO.country());
    trip.setStartDate(tripInfoRequestDTO.startDate());
    trip.setEndDate(tripInfoRequestDTO.endDate());
    trip.setHashtagsFromList(tripInfoRequestDTO.hashtags());

    Trip updatedTrip = tripRepository.save(trip);

    return new TripInfoResponseDTO(
        updatedTrip.getTripId(),
        updatedTrip.getTripTitle(),
        updatedTrip.getCountry(),
        formatLocalDateToString(updatedTrip.getStartDate()),
        formatLocalDateToString(updatedTrip.getEndDate()),
        updatedTrip.getHashtagsAsList(),
        List.of()
    );
  }

  // 사용자 여행 정보 삭제
  @Transactional
  public void deleteTrip(Long tripId) {
    Trip trip = tripRepository.findById(tripId)
        .orElseThrow(() -> new CustomException(ResultCode.TRIP_NOT_FOUND));

    // 미디어 파일 삭제
    mediaProcessingService.deleteMediaFilesByTrip(trip);

    // 여행 삭제
    tripRepository.delete(trip);
  }
}
