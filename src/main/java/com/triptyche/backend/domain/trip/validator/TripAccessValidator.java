package com.triptyche.backend.domain.trip.validator;

import com.triptyche.backend.domain.trip.model.Trip;
import com.triptyche.backend.domain.trip.repository.TripRepository;
import com.triptyche.backend.domain.user.repository.UserRepository;
import com.triptyche.backend.global.common.ResultCode;
import com.triptyche.backend.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TripAccessValidator {

  private final UserRepository userRepository;
  private final TripRepository tripRepository;

  public Trip validateAccessibleTrip(Long tripId, String userEmail) {
    // 사용자 검증
    var user = userRepository.findByUserEmail(userEmail)
            .orElseThrow(() -> new CustomException(ResultCode.USER_NOT_FOUND));

    // 여행 검증
    return tripRepository.findAccessibleTrip(tripId, user.getUserId())
            .orElseThrow(() -> new CustomException(ResultCode.UNAUTHORIZED_ACCESS));
  }
}
