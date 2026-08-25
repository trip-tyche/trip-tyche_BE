package com.triptyche.backend.domain.notification.service;

import com.triptyche.backend.domain.notification.dto.PushMessage;
import com.triptyche.backend.domain.notification.model.NotificationType;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class NotificationPushMapper {

  private static final String DEEPLINK_TRIP = "triptyche://trip/";

  public PushMessage toPushMessage(NotificationType type, Map<String, Object> payload) {
    String tripKey = text(payload.get("tripKey"));
    String tripTitle = text(payload.get("tripTitle"));
    String sender = text(payload.get("senderNickname"));
    String count = text(payload.get("count"));

    return new PushMessage(
            title(type),
            body(type, tripTitle, sender, count),
            type.name(),
            tripKey.isBlank() ? null : DEEPLINK_TRIP + tripKey,
            tripKey.isBlank() ? text(payload.get("notificationId")) : tripKey);
  }

  private String title(NotificationType type) {
    return switch (type) {
      case SHARED_REQUEST -> "새 공유 요청";
      case SHARED_APPROVE -> "공유 요청 수락";
      case SHARED_REJECTED -> "공유 요청 거절";
      case TRIP_UPDATED -> "여행 정보 변경";
      case TRIP_DELETED -> "여행 삭제";
      case MEDIA_FILE_ADDED -> "사진 추가";
      case MEDIA_FILE_UPDATED -> "사진 정보 변경";
      case MEDIA_FILE_DELETED -> "사진 삭제";
    };
  }

  private String body(NotificationType type, String tripTitle, String sender, String count) {
    String who = sender.isBlank() ? "누군가" : sender;
    String what = tripTitle.isBlank() ? "여행" : "'" + tripTitle + "'";

    return switch (type) {
      case SHARED_REQUEST -> who + "님이 " + what + " 여행을 공유했어요";
      case SHARED_APPROVE -> who + "님이 " + what + " 공유를 수락했어요";
      case SHARED_REJECTED -> who + "님이 " + what + " 공유를 거절했어요";
      case TRIP_UPDATED -> who + "님이 " + what + " 정보를 수정했어요";
      case TRIP_DELETED -> who + "님이 " + what + " 여행을 삭제했어요";
      case MEDIA_FILE_ADDED -> who + "님이 " + what + "에 사진" + amount(count) + "을 추가했어요";
      case MEDIA_FILE_UPDATED -> who + "님이 " + what + "의 사진 정보를 수정했어요";
      case MEDIA_FILE_DELETED -> who + "님이 " + what + "에서 사진" + amount(count) + "을 삭제했어요";
    };
  }

  private String amount(String count) {
    return count.isBlank() || "0".equals(count) ? "" : " " + count + "장";
  }

  private String text(Object value) {
    return value == null ? "" : String.valueOf(value);
  }
}
