package com.fivefeeling.memory.domain.pinpoint.model;

import com.fivefeeling.memory.domain.trip.model.TripInfoResponseDTO;
import java.util.List;

public record PinPointTripInfoResponseDTO(
    TripInfoResponseDTO tripInfo,
    List<PinPointResponseDTO> pinPoints
) {

  public static PinPointTripInfoResponseDTO from(TripInfoResponseDTO tripInfo, List<PinPointResponseDTO> pinPoints) {
    return new PinPointTripInfoResponseDTO(tripInfo, pinPoints);
  }
}
