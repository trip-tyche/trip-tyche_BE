package com.triptyche.backend.domain.share.dto;

public record ShareSummaryResponse(
        Long tripId,
        Long recipientId,
        Long shareId,
        String recipientNickname
) {}