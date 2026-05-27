package com.triptyche.backend.domain.share.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ShareRejectedEvent(
    Long shareId,
    Long tripId,
    String tripTitle,
    Long ownerId,
    String senderNickname
) {}