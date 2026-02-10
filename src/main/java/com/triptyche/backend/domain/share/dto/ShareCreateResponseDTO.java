package com.triptyche.backend.domain.share.dto;

import com.triptyche.backend.domain.share.model.ShareStatus;

public record ShareCreateResponseDTO(
        Long shareId,
        Long tripId,
        Long recipientId,
        ShareStatus shareStatus
) {

}
