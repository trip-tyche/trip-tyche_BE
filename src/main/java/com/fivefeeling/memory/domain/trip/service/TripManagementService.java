package com.fivefeeling.memory.domain.trip.service;

import com.fivefeeling.memory.domain.trip.model.Trip;
import com.fivefeeling.memory.domain.trip.model.TripRequestDTO;
import com.fivefeeling.memory.domain.trip.model.TripResponseDTO;
import com.fivefeeling.memory.domain.trip.model.TripUdateRequestDTO;
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

  public TripResponseDTO createTrip(String userEmail, TripRequestDTO tripRequestDTO) {
    User user = userRepository.findByUserEmail(userEmail)
        .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 존재하지 않습니다."));
    Trip trip = Trip.builder()
        .user(user)
        .tripTitle(tripRequestDTO.tripTitle())
        .country(tripRequestDTO.country())
        .startDate(tripRequestDTO.startDate())
        .endDate(tripRequestDTO.endDate())
        .build();

    trip.setHashtagsFromList(tripRequestDTO.hashtags());
    Trip savedTrip = tripRepository.save(trip);

    return new TripResponseDTO(
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
  public TripResponseDTO updateTrip(String userEmail, Long tripId, TripUdateRequestDTO tripUdateRequestDTO) {
    User user = userRepository.findByUserEmail(userEmail)
        .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 존재하지 않습니다."));

    Trip trip = tripRepository.findById(tripId)
        .orElseThrow(() -> new IllegalArgumentException("해당 여행이 존재하지 않습니다."));

    trip.setTripTitle(tripUdateRequestDTO.tripTitle());
    trip.setCountry(tripUdateRequestDTO.country());
    trip.setStartDate(tripUdateRequestDTO.startDate());
    trip.setEndDate(tripUdateRequestDTO.endDate());
    trip.setHashtagsFromList(tripUdateRequestDTO.hashtags());

    Trip updatedTrip = tripRepository.save(trip);

    return new TripResponseDTO(
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
