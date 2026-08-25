package com.triptyche.backend.domain.notification.dto;

public record PushMessage(String title, String body, String type, String deeplink, String resourceId) {
}
