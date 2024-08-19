package com.fivefeeling.memory.service;

import com.fivefeeling.memory.dto.PinPointSummaryDTO;
import com.fivefeeling.memory.dto.TripSummaryDTO;
import com.fivefeeling.memory.repository.PinPointRepository;
import com.fivefeeling.memory.repository.TripRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TripService {

  private final TripRepository tripRepository;
  private final PinPointRepository pinPointRepository;

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
}
