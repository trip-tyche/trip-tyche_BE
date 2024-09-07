package com.fivefeeling.memory.domain.user.model;

import com.fivefeeling.memory.domain.trip.model.TripInfoDTO;
import java.util.List;

public record UserTripsDTO(
    String userNickName,
    List<TripInfoDTO> trips
) {

}
