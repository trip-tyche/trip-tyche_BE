package com.fivefeeling.memory.domain.share.dto;

import com.fivefeeling.memory.domain.share.model.ShareStatus;

public record ShareCreateResponseDTO(
        Long shareId,
        Long tripId,
        Long recipientId,
        ShareStatus shareStatus
) {

}
