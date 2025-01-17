package com.fivefeeling.memory.domain.share.dto;

public record ShareCreateRequestDTO(
        Long tripId,
        Long recipientId
) {

}
