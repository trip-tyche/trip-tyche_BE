package com.triptyche.backend.domain.media.event;

public record MediaFileZeroLocationCacheRequestedEvent(
        Long tripId, Long mediaFileId, String mediaLink, String recordDate) {}