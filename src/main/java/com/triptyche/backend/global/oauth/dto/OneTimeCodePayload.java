package com.triptyche.backend.global.oauth.dto;

public record OneTimeCodePayload(String userEmail, String provider) {
}
