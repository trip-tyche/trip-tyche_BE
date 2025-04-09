package com.fivefeeling.memory.domain.trip.validator;

import com.fivefeeling.memory.domain.trip.model.Trip;
import com.fivefeeling.memory.domain.trip.repository.TripRepository;
import com.fivefeeling.memory.domain.user.repository.UserRepository;
import com.fivefeeling.memory.global.common.ResultCode;
import com.fivefeeling.memory.global.exception.CustomException;
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
