package com.fivefeeling.memory.domain.notification.dto;

import java.time.LocalDateTime;

public record NotificationResponseDTO(
        Long notificationId,
        Long referenceId,
        String message,
        String status,
        String senderNickname,
        LocalDateTime createdAt
) {

}
