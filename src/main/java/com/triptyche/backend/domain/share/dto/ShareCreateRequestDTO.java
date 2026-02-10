package com.triptyche.backend.domain.share.dto;

public record ShareCreateRequestDTO(
        String tripKey,
        Long recipientId
) {

}
