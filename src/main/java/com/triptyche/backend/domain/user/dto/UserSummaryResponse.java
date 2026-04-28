package com.triptyche.backend.domain.user.dto;

import com.triptyche.backend.domain.user.model.UserRole;

public record UserSummaryResponse(
        Long userId,
        String nickname,
        long tripsCount,
        long unreadNotificationsCount,
        UserRole role
) {

}
