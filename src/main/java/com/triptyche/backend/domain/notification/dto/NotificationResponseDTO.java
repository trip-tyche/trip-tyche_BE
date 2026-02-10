package com.triptyche.backend.domain.notification.dto;

public record NotificationResponseDTO(
        Long notificationId,
        Long referenceId,
        String message,
        String status,
        String senderNickname,
        String createdAt
) {

}
