package com.triptyche.backend.domain.notification.dto;

public record NotificationResponse(
        Long notificationId,
        Long referenceId,
        String message,
        String status,
        String senderNickname,
        String createdAt
) {

}