package com.fivefeeling.memory.domain.share.dto;

import com.fivefeeling.memory.domain.share.model.ShareStatus;
import lombok.Builder;

@Builder
public record ShareResponseDTO(
        Long shareId,
        String tripTitle,
        String ownerNickname,
        String recipientNickname,
        ShareStatus status,
        String country,
        String startDate,
        String endDate,
        String hashtags
) {

}
