package com.fivefeeling.memory.domain.trip.dto;

import com.fivefeeling.memory.domain.media.dto.MediaFileResponseDTO;
import com.fivefeeling.memory.domain.pinpoint.model.PinPointResponseDTO;
import java.util.List;

public record MapViewResponseDTO(
        String tripTitle,
        String startDate,
        String endDate,
        List<PinPointResponseDTO> pinPoints,
        List<MediaFileResponseDTO> mediaFiles
) {

}
