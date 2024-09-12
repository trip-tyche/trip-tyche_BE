package com.fivefeeling.memory.domain.trip.service;

import com.fivefeeling.memory.domain.trip.model.Trip;
import com.fivefeeling.memory.domain.trip.model.TripInfoRequestDTO;
import com.fivefeeling.memory.domain.trip.model.TripInfoResponseDTO;
import com.fivefeeling.memory.domain.trip.repository.TripRepository;
import com.fivefeeling.memory.domain.user.model.User;
import com.fivefeeling.memory.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TripManagementService {

  private final TripRepository tripRepository;
  private final UserRepository userRepository;

  public TripInfoResponseDTO createTrip(String userEmail, TripInfoRequestDTO tripInfoRequestDTO) {
    User user = userRepository.findByUserEmail(userEmail)
        .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 존재하지 않습니다."));
    Trip trip = Trip.builder()
        .user(user)
        .tripTitle(tripInfoRequestDTO.tripTitle())
        .country(tripInfoRequestDTO.country())
        .startDate(tripInfoRequestDTO.startDate())
        .endDate(tripInfoRequestDTO.endDate())
        .build();

    trip.setHashtagsFromList(tripInfoRequestDTO.hashtags());
    Trip savedTrip = tripRepository.save(trip);

    return new TripInfoResponseDTO(
        savedTrip.getTripId(),
        savedTrip.getTripTitle(),
        savedTrip.getCountry(),
        savedTrip.getStartDate(),
        savedTrip.getEndDate(),
        savedTrip.getHashtagsAsList()
    );
  }

  // 사용자 여행 정보 수정
  @Transactional
  public TripInfoResponseDTO updateTrip(String userEmail, Long tripId, TripInfoRequestDTO tripInfoRequestDTO) {
    User user = userRepository.findByUserEmail(userEmail)
        .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 존재하지 않습니다."));

    Trip trip = tripRepository.findById(tripId)
        .orElseThrow(() -> new IllegalArgumentException("해당 여행이 존재하지 않습니다."));

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
        updatedTrip.getStartDate(),
        updatedTrip.getEndDate(),
        updatedTrip.getHashtagsAsList()
    );
  }

  // 사용자 여행 정보 삭제
  @Transactional
  public void deleteTrip(String userEmail, Long tripId) {
    User user = userRepository.findByUserEmail(userEmail)
        .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 존재하지 않습니다."));

    Trip trip = tripRepository.findById(tripId)
        .orElseThrow(() -> new IllegalArgumentException("해당 여행이 존재하지 않습니다."));

    tripRepository.delete(trip);
  }
}
