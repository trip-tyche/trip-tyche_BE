package com.triptyche.backend.domain.user.dto;

import com.triptyche.backend.domain.user.model.UserRole;
import java.time.LocalDateTime;

public record UserSummaryResponse(
        Long userId,
        String nickname,
        long tripsCount,
        long unreadNotificationsCount,
        UserRole role,
        LocalDateTime createdAt
) {

}
