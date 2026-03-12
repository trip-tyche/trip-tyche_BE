package com.triptyche.backend.domain.trip.validator;

import com.triptyche.backend.domain.trip.model.Trip;
import com.triptyche.backend.domain.trip.repository.TripRepository;
import com.triptyche.backend.domain.user.model.User;
import com.triptyche.backend.domain.user.repository.UserRepository;
import com.triptyche.backend.global.common.ResultCode;
import com.triptyche.backend.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TripAccessValidator {

  private final UserRepository userRepository;
  private final TripRepository tripRepository;

  public Trip validateAccessibleTrip(Long tripId, String userEmail) {
    var user = userRepository.findByUserEmail(userEmail)
            .orElseThrow(() -> new CustomException(ResultCode.USER_NOT_FOUND));

    return tripRepository.findAccessibleTrip(tripId, user.getUserId())
            .orElseThrow(() -> new CustomException(ResultCode.UNAUTHORIZED_ACCESS));
  }

  public Trip validateOwner(Long tripId, String userEmail) {
    var user = userRepository.findByUserEmail(userEmail)
            .orElseThrow(() -> new CustomException(ResultCode.USER_NOT_FOUND));

    var trip = tripRepository.findById(tripId)
            .orElseThrow(() -> new CustomException(ResultCode.TRIP_NOT_FOUND));

    if (!trip.isOwnedBy(user.getUserId())) {
      throw new CustomException(ResultCode.UNAUTHORIZED_ACCESS);
    }

    return trip;
  }

  public TripAccessResult validateWithUser(Long tripId, String userEmail) {
    var user = userRepository.findByUserEmail(userEmail)
            .orElseThrow(() -> new CustomException(ResultCode.USER_NOT_FOUND));

    var trip = tripRepository.findAccessibleTrip(tripId, user.getUserId())
            .orElseThrow(() -> new CustomException(ResultCode.UNAUTHORIZED_ACCESS));

    return new TripAccessResult(trip, user);
  }
}