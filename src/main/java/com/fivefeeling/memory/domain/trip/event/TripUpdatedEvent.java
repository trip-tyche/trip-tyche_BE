package com.fivefeeling.memory.domain.trip.event;

import com.fivefeeling.memory.domain.trip.model.Trip;

public record TripUpdatedEvent(
        Trip trip,
        Long actorId,
        String actorNickname,
        boolean isOwner) {

}
