package com.triptyche.backend.domain.trip.event;

import com.triptyche.backend.domain.trip.model.Trip;

public record TripDeletedEvent(Trip trip) {

}
