package com.triptyche.backend.domain.trip.event;

public record TripDeletedEvent(
    Long tripId,
    String tripTitle,
    String ownerNickname,
    Long ownerId
) {}