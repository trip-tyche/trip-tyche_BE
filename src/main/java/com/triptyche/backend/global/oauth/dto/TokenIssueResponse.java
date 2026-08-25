package com.triptyche.backend.global.oauth.dto;

public record TokenIssueResponse(String accessToken, String refreshToken, long expiresIn) {
}
