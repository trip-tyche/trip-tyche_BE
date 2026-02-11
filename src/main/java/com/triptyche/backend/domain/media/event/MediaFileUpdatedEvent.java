package com.triptyche.backend.domain.media.event;

import com.triptyche.backend.domain.trip.model.Trip;

public record MediaFileUpdatedEvent(
        Trip trip,
        Long actorId,
        String actorNickname,
        boolean isOwner,
        int count) {

}
