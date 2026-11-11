package com.triptyche.backend.domain.user.event;

public record UserRegisteredEvent(
    Long userId,
    String provider,
    boolean guest
) {}
