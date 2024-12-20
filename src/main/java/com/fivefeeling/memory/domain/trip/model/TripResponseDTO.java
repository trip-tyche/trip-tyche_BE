package com.fivefeeling.memory.domain.trip.model;

import com.fivefeeling.memory.domain.media.model.MediaFileResponseDTO;
import com.fivefeeling.memory.domain.pinpoint.model.PinPointResponseDTO;
import java.util.List;

public record TripResponseDTO(
    String tripTitle,
    String startDate,
    String endDate,
    List<PinPointResponseDTO> pinPoints,
    List<MediaFileResponseDTO> mediaFiles
) {

}
