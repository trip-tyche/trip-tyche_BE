package com.triptyche.backend.domain.trip.converter;

import com.triptyche.backend.domain.trip.model.Trip;
import com.triptyche.backend.domain.trip.repository.TripRepository;
import com.triptyche.backend.global.common.ResultCode;
import com.triptyche.backend.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TripKeyConverter {

  private final TripRepository tripRepository;

  /**
   * 클라이언트로 전달받은 tripKey를 DB에 저장된 tripKey로 변환하는 메서드<br>
   * 만약 일치하는 Trip이 없다면 403 Forbidden 에러를 발생시킴
   *
   * @param tripKey
   *         클라이언트로부터 전달받은 tripKey
   * @return DB에 저장된 tripId
   * @throws CustomException
   *         UNAUTHORIZED_ACCESS 상태 (403)
   */
  public Long convertToTripId(String tripKey) {
    Trip trip = tripRepository.findByTripKey(tripKey)
            .orElseThrow(() -> new CustomException(ResultCode.UNAUTHORIZED_ACCESS));
    return trip.getTripId();
  }
}
