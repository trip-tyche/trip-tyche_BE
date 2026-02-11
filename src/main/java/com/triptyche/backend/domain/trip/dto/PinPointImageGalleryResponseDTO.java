package com.triptyche.backend.domain.trip.dto;

import com.triptyche.backend.domain.media.dto.PinPointMediaFilesResponseDTO;
import java.util.List;

public record PinPointImageGalleryResponseDTO(
        Long pinPointId,
        List<PinPointMediaFilesResponseDTO> mediaFiles
) {

}
