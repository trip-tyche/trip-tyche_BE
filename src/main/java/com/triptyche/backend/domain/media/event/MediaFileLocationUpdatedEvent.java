package com.triptyche.backend.domain.media.event;

public record MediaFileLocationUpdatedEvent(
        Long tripId,
        String tripTitle,
        String tripKey,
        Long ownerId,
        Long mediaFileId,
        Long actorId,
        String actorNickname,
        boolean isOwner) {

}