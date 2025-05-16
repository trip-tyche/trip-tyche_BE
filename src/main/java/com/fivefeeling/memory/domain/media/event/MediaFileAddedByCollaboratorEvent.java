package com.fivefeeling.memory.domain.media.event;

import com.fivefeeling.memory.domain.trip.model.Trip;

public record MediaFileAddedByCollaboratorEvent(Trip trip, Long collaboratorId, String collaboratorNickname) {

}
