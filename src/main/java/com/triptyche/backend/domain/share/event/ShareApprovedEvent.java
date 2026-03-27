package com.triptyche.backend.domain.share.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ShareApprovedEvent(
    Long shareId,
    Long tripId,
    Long ownerId,
    String senderNickname
) {}