package com.fivefeeling.memory.dto;

import java.util.List;

public record TripResponseDTO(
    Long id,
    String tripTitle,
    String country,
    String startDate,
    String endDate,
    List<String> hashtags
) {

}
