package com.fivefeeling.memory.service;

import com.fivefeeling.memory.dto.PinPointSummaryDTO;
import com.fivefeeling.memory.dto.TripRequestDTO;
import com.fivefeeling.memory.dto.TripResponseDTO;
import com.fivefeeling.memory.dto.TripSummaryDTO;
import com.fivefeeling.memory.entity.Trip;
import com.fivefeeling.memory.entity.User;
import com.fivefeeling.memory.repository.PinPointRepository;
import com.fivefeeling.memory.repository.TripRepository;
import com.fivefeeling.memory.repository.UserRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TripService {

  private final TripRepository tripRepository;
  private final PinPointRepository pinPointRepository;
  private final UserRepository userRepository;

  public List<TripSummaryDTO> getTripsByUserId(Long userId) {
    return tripRepository.findByUserUserId(userId).stream()
        .map(trip -> new TripSummaryDTO(
            trip.getTripId(),
            trip.getCountry()
        ))
        .collect(Collectors.toList());
  }

  public List<PinPointSummaryDTO> getPinPointsByUserId(Long userId) {
    return tripRepository.findByUserUserId(userId).stream()
        .map(trip -> pinPointRepository.findByTripTripId(trip.getTripId()).stream().findFirst().orElse(null))
        .filter(pinPoint -> pinPoint != null)
        .map(pinPoint -> new PinPointSummaryDTO(
            pinPoint.getTrip().getTripId(),
            pinPoint.getPinPointId(),
            pinPoint.getLatitude(),
            pinPoint.getLongitude()
        ))
        .collect(Collectors.toList());
  }

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
}
