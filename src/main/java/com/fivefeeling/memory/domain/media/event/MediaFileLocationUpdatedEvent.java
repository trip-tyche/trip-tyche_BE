package com.fivefeeling.memory.domain.media.event;

import com.fivefeeling.memory.domain.trip.model.Trip;

public record MediaFileLocationUpdatedEvent(Trip trip, Long mediaFileId) {

}
