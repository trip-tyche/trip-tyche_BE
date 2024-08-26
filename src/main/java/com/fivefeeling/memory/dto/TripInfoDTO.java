package com.fivefeeling.memory.dto;

import java.time.LocalDate;
import java.util.List;

public record TripInfoDTO(
    Long tripId,
    String tripTitle,
    String country,
    LocalDate startDate,
    LocalDate endDate,
    List<String> hashtags
) {

}
