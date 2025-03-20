package com.fivefeeling.memory.domain.user.dto;

import com.fivefeeling.memory.domain.pinpoint.model.PinPointResponseDTO;
import com.fivefeeling.memory.domain.trip.model.TripInfoResponseDTO;
import java.util.List;

public record UserTripInfoResponseDTO(
        Long userId,
        String userNickName,
        List<TripInfoResponseDTO> trips,
        List<PinPointResponseDTO> pinPoints
) {

  public static UserTripInfoResponseDTO withoutPinPoints(Long userId, String userNickName,
                                                         List<TripInfoResponseDTO> trips) {
    return new UserTripInfoResponseDTO(userId, userNickName, trips, null);
  }
}
