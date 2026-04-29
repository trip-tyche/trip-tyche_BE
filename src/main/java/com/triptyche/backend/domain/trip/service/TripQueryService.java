package com.triptyche.backend.domain.trip.service;

import com.triptyche.backend.domain.trip.model.Trip;
import com.triptyche.backend.domain.trip.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TripQueryService {

  private final TripRepository tripRepository;

  public String getTripTitleById(Long tripId) {
    return tripRepository.findById(tripId)
            .map(Trip::getTripTitle)
            .orElse("UNKNOWN_TRIP");
  }
}
