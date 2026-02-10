package com.triptyche.backend.domain.user.dto;

public record UserSummaryResponseDTO(
        Long userId,
        String nickname,
        long tripsCount,
        long unreadNotificationsCount
) {

}

