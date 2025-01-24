package com.fivefeeling.memory.domain.notification.dto;

import java.time.LocalDateTime;

public record NotificationResponseDTO(
        Long notificationId,
        String title,
        String status,
        LocalDateTime createdAt
) {

}
