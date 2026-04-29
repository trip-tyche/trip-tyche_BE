package com.triptyche.backend.domain.notification.event;

import com.triptyche.backend.domain.notification.model.NotificationType;
import java.util.Map;

public record NotificationSavedEvent(
    Long recipientId,
    NotificationType type,
    Map<String, Object> payload
) {}