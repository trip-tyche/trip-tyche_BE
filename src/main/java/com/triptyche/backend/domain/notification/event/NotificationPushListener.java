package com.triptyche.backend.domain.notification.event;

import com.triptyche.backend.domain.device.service.DeviceService;
import com.triptyche.backend.domain.notification.dto.PushMessage;
import com.triptyche.backend.domain.notification.service.FcmSender;
import com.triptyche.backend.domain.notification.service.NotificationPushMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationPushListener {

  private final DeviceService deviceService;
  private final NotificationPushMapper pushMapper;
  private final FcmSender fcmSender;

  @Async("pushTaskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onNotificationSaved(NotificationSavedEvent event) {
    try {
      List<String> tokens = deviceService.findTokens(event.recipientId());

      if (tokens.isEmpty()) {
        return;
      }

      PushMessage message = pushMapper.toPushMessage(event.type(), event.payload());
      deviceService.removeInvalidTokens(fcmSender.send(tokens, message));
    } catch (Exception e) {
      log.error("[{}] 푸시 발송 실패: recipient={}", event.type(), event.recipientId(), e);
    }
  }
}
