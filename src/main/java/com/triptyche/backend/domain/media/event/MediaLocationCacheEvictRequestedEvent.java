package com.triptyche.backend.domain.media.event;

public record MediaLocationCacheEvictRequestedEvent(Long tripId, Long mediaFileId) {}