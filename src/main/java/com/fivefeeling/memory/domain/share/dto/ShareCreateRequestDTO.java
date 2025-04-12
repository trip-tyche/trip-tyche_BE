package com.fivefeeling.memory.domain.share.dto;

public record ShareCreateRequestDTO(
        String tripKey,
        Long recipientId
) {

}
