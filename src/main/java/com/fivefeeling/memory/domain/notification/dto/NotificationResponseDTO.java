package com.fivefeeling.memory.domain.notification.dto;

import java.time.LocalDateTime;

public record NotificationResponseDTO(
        Long notificationId,
        Long shareId,
        String message,
        String status,
        LocalDateTime createdAt
) {

}
