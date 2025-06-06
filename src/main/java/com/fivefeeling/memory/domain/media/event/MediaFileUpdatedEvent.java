package com.fivefeeling.memory.domain.media.event;

import com.fivefeeling.memory.domain.trip.model.Trip;

public record MediaFileUpdatedEvent(
        Trip trip,
        Long actorId,
        String actorNickname,
        boolean isOwner,
        int count) {

}
