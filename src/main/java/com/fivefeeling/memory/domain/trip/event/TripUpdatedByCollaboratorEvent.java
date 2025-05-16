package com.fivefeeling.memory.domain.trip.event;

import com.fivefeeling.memory.domain.trip.model.Trip;

public record TripUpdatedByCollaboratorEvent(Trip trip, Long collaboratorId, String collaboratorNickname) {

}
