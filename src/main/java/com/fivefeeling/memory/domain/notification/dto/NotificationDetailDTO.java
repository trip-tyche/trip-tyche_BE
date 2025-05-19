package com.fivefeeling.memory.domain.notification.dto;

import com.fivefeeling.memory.domain.notification.model.NotificationType;

public record NotificationDetailDTO(String tripTitle,
                                    NotificationType message,
                                    String senderNickname) {

}
