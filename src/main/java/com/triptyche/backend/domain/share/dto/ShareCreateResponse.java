package com.triptyche.backend.domain.share.dto;

import com.triptyche.backend.domain.share.model.ShareStatus;

public record ShareCreateResponse(
        Long shareId,
        Long tripId,
        Long recipientId,
        ShareStatus shareStatus
) {

}