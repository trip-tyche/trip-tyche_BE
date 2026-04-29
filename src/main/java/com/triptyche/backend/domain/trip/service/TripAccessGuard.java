package com.triptyche.backend.domain.trip.service;

import com.triptyche.backend.domain.trip.model.Trip;
import com.triptyche.backend.domain.trip.repository.TripRepository;
import com.triptyche.backend.domain.user.model.User;
import com.triptyche.backend.global.common.ResultCode;
import com.triptyche.backend.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TripAccessGuard {

  private final TripRepository tripRepository;

  public Trip validateAccessibleTripByKey(String tripKey, User user) {
    return tripRepository.findAccessibleTripByKey(tripKey, user.getUserId())
            .orElseThrow(() -> new CustomException(ResultCode.ACCESS_DENIED));
  }

  public Trip validateOwnerByKey(String tripKey, User user) {
    return tripRepository.findOwnerTripByKey(tripKey, user.getUserId())
            .orElseThrow(() -> new CustomException(ResultCode.ACCESS_DENIED));
  }
}
