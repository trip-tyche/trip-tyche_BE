package com.fivefeeling.memory.dto;

import java.time.LocalDate;
import java.util.List;

public record TripResponseDTO(
    Long id,
    String tripTitle,
    String country,
    LocalDate startDate,
    LocalDate endDate,
    List<String> hashtags
) {

}
