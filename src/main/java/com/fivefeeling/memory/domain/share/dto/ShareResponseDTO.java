package com.fivefeeling.memory.domain.share.dto;

import com.fivefeeling.memory.domain.share.model.ShareStatus;
import lombok.Builder;

@Builder
public record ShareResponseDTO(
        Long shareId,
        Long tripId,
        String tripTitle,
        String ownerNickname,
        ShareStatus status
) {

}
