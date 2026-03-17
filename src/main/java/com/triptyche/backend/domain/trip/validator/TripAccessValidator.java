package com.triptyche.backend.domain.trip.validator;

import com.triptyche.backend.domain.trip.model.Trip;
import com.triptyche.backend.domain.trip.repository.TripRepository;
import com.triptyche.backend.domain.user.model.User;
import com.triptyche.backend.global.common.ResultCode;
import com.triptyche.backend.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TripAccessValidator {

  private final TripRepository tripRepository;

  public Trip validateAccessibleTrip(Long tripId, User user) {
    return tripRepository.findAccessibleTrip(tripId, user.getUserId())
            .orElseThrow(() -> new CustomException(ResultCode.UNAUTHORIZED_ACCESS));
  }

  public Trip validateOwner(Long tripId, User user) {
    return tripRepository.findByTripIdAndOwner(tripId, user.getUserId())
            .orElseThrow(() -> new CustomException(ResultCode.UNAUTHORIZED_ACCESS));
  }
}