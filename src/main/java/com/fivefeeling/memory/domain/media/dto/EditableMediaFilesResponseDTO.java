package com.fivefeeling.memory.domain.media.dto;

import java.util.List;

public record EditableMediaFilesResponseDTO(
        String startDate,
        String endDate,
        List<EditableMediaFileResponseDTO> mediaFiles
) {

}
