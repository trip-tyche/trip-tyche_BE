package com.triptyche.backend.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "triptyche.guest")
public record GuestProperties(
    String templateEmail,
    String shareTargetTripTitle,
    long shareDelaySeconds,
    String sharePollCron
) {}