package com.triptyche.backend.domain.share.dto;

public record ShareSummary(
        Long tripId,
        Long recipientId,
        Long shareId,
        String recipientNickname
) {}