package com.fivefeeling.memory.domain.notification.dto;

import java.time.LocalDateTime;

public record NotificationResponseDTO(
        Long notificationId,
        String message,
        String status,
        LocalDateTime createdAt
) {

}
