package com.fivefeeling.memory.domain.trip.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record TripInfoRequestDTO(
        @NotBlank String tripTitle,
        @NotBlank String country,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        List<String> hashtags
) {

}
