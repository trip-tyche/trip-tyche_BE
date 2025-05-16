package com.fivefeeling.memory.domain.trip.event;

import com.fivefeeling.memory.domain.trip.model.Trip;

public record TripDeletedEvent(Trip trip) {

}
