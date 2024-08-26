package com.fivefeeling.memory.dto;

import java.util.List;

public record UserTripsDTO(
    String userNickName,
    List<TripInfoDTO> trips
) {

}
