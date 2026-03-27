package com.triptyche.backend.domain.share.dto;

public record ShareCreateRequest(
        String tripKey,
        Long recipientId
) {

}