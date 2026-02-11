package com.triptyche.backend.domain.media.dto;

import java.util.List;

public record MediaFileBatchDeleteRequestDTO(
        List<Long> mediaFileIds
) {

}
