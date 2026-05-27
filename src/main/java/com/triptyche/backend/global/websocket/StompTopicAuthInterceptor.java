package com.triptyche.backend.global.websocket;

import com.triptyche.backend.domain.user.model.User;
import com.triptyche.backend.domain.user.repository.UserRepository;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StompTopicAuthInterceptor implements ChannelInterceptor {

  private static final String MEDIA_PROCESSED_PREFIX = "/topic/media-processed/";

  private final UserRepository userRepository;

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

    if (accessor.getCommand() != StompCommand.SUBSCRIBE) {
      return message;
    }

    String destination = accessor.getDestination();
    if (destination == null || !destination.startsWith(MEDIA_PROCESSED_PREFIX)) {
      return message;
    }

    String pathUserId = destination.substring(MEDIA_PROCESSED_PREFIX.length());
    long requestedUserId;
    try {
      requestedUserId = Long.parseLong(pathUserId);
    } catch (NumberFormatException e) {
      log.warn("STOMP SUBSCRIBE 거부 — 유효하지 않은 userId 형식: destination={}", destination);
      throw new MessageDeliveryException("invalid topic");
    }

    Principal principal = accessor.getUser();
    if (principal == null) {
      log.warn("STOMP SUBSCRIBE 거부 — 인증되지 않은 사용자: destination={}", destination);
      throw new MessageDeliveryException("forbidden topic subscription");
    }

    String userEmail = principal.getName();
    User user = userRepository.findByUserEmail(userEmail).orElse(null);
    if (user == null || !user.getUserId().equals(requestedUserId)) {
      log.warn("STOMP SUBSCRIBE 거부 — 권한 없음: email={}, requestedUserId={}", userEmail, requestedUserId);
      throw new MessageDeliveryException("forbidden topic subscription");
    }

    return message;
  }
}
