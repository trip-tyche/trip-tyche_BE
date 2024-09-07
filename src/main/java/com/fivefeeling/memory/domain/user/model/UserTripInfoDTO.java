package com.fivefeeling.memory.domain.user.model;

import com.fivefeeling.memory.domain.pinpoint.model.PinPointSummaryDTO;
import com.fivefeeling.memory.domain.trip.model.TripSummaryDTO;
import java.util.List;

public record UserTripInfoDTO(
    Long userId,
    String userNickname,
    List<TripSummaryDTO> trips,
    List<PinPointSummaryDTO> pinPoints
) {

}