package com.triptyche.backend.domain.media.event;

public record MediaFileRegisteredEvent(
        Long mediaFileId,
        String originalKey
) {}