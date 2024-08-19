package com.fivefeeling.memory.dto;

import java.util.List;

public record UserTripInfoDTO(
    Long userId,
    String userNickname,
    List<TripSummaryDTO> trips,
    List<PinPointSummaryDTO> pinPoints
) {

}