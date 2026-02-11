package com.triptyche.backend.domain.notification.dto;

import com.triptyche.backend.domain.notification.model.NotificationType;

public record NotificationDetailDTO(String tripTitle,
                                    NotificationType message,
                                    String senderNickname) {

}
