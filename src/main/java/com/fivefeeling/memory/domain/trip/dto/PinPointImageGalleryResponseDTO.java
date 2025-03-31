package com.fivefeeling.memory.domain.trip.dto;

import com.fivefeeling.memory.domain.media.dto.PinPointMediaFilesResponseDTO;
import java.util.List;

public record PinPointImageGalleryResponseDTO(
        Long pinPointId,
        List<PinPointMediaFilesResponseDTO> mediaFiles
) {

}
