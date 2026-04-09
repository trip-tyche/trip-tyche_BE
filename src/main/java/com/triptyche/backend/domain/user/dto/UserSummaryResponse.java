package com.triptyche.backend.domain.user.dto;

public record UserSummaryResponse(
        Long userId,
        String nickname,
        long tripsCount,
        long unreadNotificationsCount
) {

}