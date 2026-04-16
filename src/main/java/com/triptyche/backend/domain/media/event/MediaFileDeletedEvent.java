package com.triptyche.backend.domain.media.event;

public record MediaFileDeletedEvent(
        Long tripId,
        String tripTitle,
        String tripKey,
        Long ownerId,
        Long actorId,
        String actorNickname,
        boolean isOwner,
        int count) {

}
