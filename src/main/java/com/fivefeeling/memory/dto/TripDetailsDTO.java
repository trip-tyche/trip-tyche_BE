package com.fivefeeling.memory.dto;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public record TripDetailsDTO(
    Long tripId,
    String tripTitle,
    String country,
    LocalDate startDate,
    LocalDate endDate,
    List<PinPointMediaDTO> pinPoints
) {


  private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  public String formattedStartDate() {
    return startDate != null ? startDate.format(dateFormatter) : null;
  }

  public String formattedEndDate() {
    return endDate != null ? endDate.format(dateFormatter) : null;
  }
}
