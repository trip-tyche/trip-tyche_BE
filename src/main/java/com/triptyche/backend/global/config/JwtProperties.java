package com.triptyche.backend.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        long accessTokenExpirySeconds,
        long refreshTokenExpirySeconds
) {}