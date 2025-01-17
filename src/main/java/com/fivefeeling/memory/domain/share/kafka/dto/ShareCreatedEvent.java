package com.fivefeeling.memory.domain.share.kafka.dto;

public record ShareCreatedEvent(
        Long shareId,
        Long tripId,
        String tripTitle,
        String ownerNickname,
        Long recipientId,
        String status,
        String action
) {

}
