package com.triptyche.backend.domain.media.event;

import com.triptyche.backend.domain.trip.model.Trip;

public record MediaFileLocationUpdatedEvent(Trip trip, Long mediaFileId) {

}
