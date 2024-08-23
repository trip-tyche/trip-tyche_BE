package com.fivefeeling.memory.dto;

import java.util.Date;
import java.util.List;

public record TripRequestDTO(
    String tripTitle,
    String country,
    Date startDate,
    Date endDate,
    List<String> hashtags
) {

}
