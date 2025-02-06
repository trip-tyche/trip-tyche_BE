package com.fivefeeling.memory.domain.media.dto;

import java.util.List;

public record TripImagesResponseDTO(
        String tripTitle,
        String startDate,
        String endDate,
        List<ImageFileResponseDTO> mediaFiles
) {

}
