package com.triptyche.backend.domain.trip.event;

import com.triptyche.backend.domain.trip.model.Trip;

public record TripUpdatedEvent(
        Trip trip,
        Long actorId,
        String actorNickname,
        boolean isOwner) {

}
