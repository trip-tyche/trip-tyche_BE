package com.triptyche.backend.domain.share.dto;

import com.triptyche.backend.domain.share.model.ShareStatus;
import java.util.List;
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
        List<String> hashtags
) {

}
