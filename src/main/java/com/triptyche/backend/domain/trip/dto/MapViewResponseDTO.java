package com.triptyche.backend.domain.trip.dto;

import com.triptyche.backend.domain.media.dto.MediaFileResponseDTO;
import java.util.List;

public record MapViewResponseDTO(
        String tripTitle,
        String startDate,
        String endDate,
        List<PinPointResponseDTO> pinPoints,
        List<MediaFileResponseDTO> mediaFiles
) {

}
