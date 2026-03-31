package com.triptyche.backend.domain.notification.dto;

import com.triptyche.backend.domain.notification.model.NotificationType;

public record NotificationDetailResponse(String tripTitle,
                                         NotificationType message,
                                         String senderNickname) {

}