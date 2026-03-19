package com.triptyche.backend.domain.trip.event;

public record TripUpdatedEvent(
        Long tripId,
        String tripKey,
        String tripTitle,
        Long ownerId,
        Long actorId,
        String actorNickname,
        boolean isOwner) {

}
