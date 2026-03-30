package com.triptyche.backend.domain.share.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ShareCreatedEvent(
    Long shareId,
    Long tripId,
    Long recipientId,
    String senderNickname,
    String tripTitle
) {}