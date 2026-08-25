package com.triptyche.backend.domain.notification.service;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import com.triptyche.backend.domain.notification.dto.PushMessage;
import com.triptyche.backend.global.config.FirebaseConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FcmSender {

  public List<String> send(List<String> tokens, PushMessage message) {
    if (tokens.isEmpty() || !FirebaseConfig.isPushAvailable()) {
      return List.of();
    }

    MulticastMessage multicast = MulticastMessage.builder()
            .addAllTokens(tokens)
            .setNotification(Notification.builder()
                    .setTitle(message.title())
                    .setBody(message.body())
                    .build())
            .putAllData(data(message))
            .build();

    try {
      BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(multicast);
      log.info("FCM 발송: 성공={}, 실패={}", response.getSuccessCount(), response.getFailureCount());
      return invalidTokens(tokens, response);
    } catch (FirebaseMessagingException e) {
      log.error("FCM 발송 실패: {}", e.getMessage());
      return List.of();
    }
  }

  // FCM은 data 값이 문자열이 아니면 발송을 거부한다.
  private Map<String, String> data(PushMessage message) {
    Map<String, String> data = new HashMap<>();
    data.put("type", message.type());

    if (message.resourceId() != null) {
      data.put("resourceId", message.resourceId());
    }
    if (message.deeplink() != null) {
      data.put("deeplink", message.deeplink());
    }
    return data;
  }

  private List<String> invalidTokens(List<String> tokens, BatchResponse response) {
    if (response.getFailureCount() == 0) {
      return List.of();
    }

    List<SendResponse> responses = response.getResponses();
    List<String> invalid = new ArrayList<>();

    for (int i = 0; i < responses.size(); i++) {
      SendResponse each = responses.get(i);

      if (each.isSuccessful() || each.getException() == null) {
        continue;
      }

      MessagingErrorCode code = each.getException().getMessagingErrorCode();
      if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT) {
        invalid.add(tokens.get(i));
      }
    }
    return invalid;
  }
}
