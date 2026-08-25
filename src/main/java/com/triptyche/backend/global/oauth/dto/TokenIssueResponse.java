package com.triptyche.backend.global.oauth.dto;

// FE가 data.accessToken / data.refreshToken / data.expiresIn 으로 읽는다. 필드명을 바꾸면 앱 로그인이 깨진다.
public record TokenIssueResponse(String accessToken, String refreshToken, long expiresIn) {
}
