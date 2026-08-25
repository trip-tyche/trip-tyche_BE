package com.triptyche.backend.global.websocket;

import com.triptyche.backend.domain.user.model.User;
import com.triptyche.backend.domain.user.repository.UserRepository;
import java.security.Principal;
import java.util.List;
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

  // 사용자별 토픽을 새로 만들면 여기에 등록해야 가드가 걸린다.
  private static final List<String> USER_SCOPED_PREFIXES = List.of(
          "/topic/media-processed/",
          "/topic/share-notifications/"
  );

  private final UserRepository userRepository;

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

    if (accessor.getCommand() != StompCommand.SUBSCRIBE) {
      return message;
    }

    String destination = accessor.getDestination();
    String prefix = matchedPrefix(destination);
    if (prefix == null) {
      return message;
    }

    verifyOwner(destination, destination.substring(prefix.length()), accessor.getUser());
    return message;
  }

  private String matchedPrefix(String destination) {
    if (destination == null) {
      return null;
    }
    return USER_SCOPED_PREFIXES.stream()
            .filter(destination::startsWith)
            .findFirst()
            .orElse(null);
  }

  private void verifyOwner(String destination, String pathUserId, Principal principal) {
    long requestedUserId;
    try {
      requestedUserId = Long.parseLong(pathUserId);
    } catch (NumberFormatException e) {
      log.warn("STOMP SUBSCRIBE 거부 — 유효하지 않은 userId 형식: destination={}", destination);
      throw new MessageDeliveryException("invalid topic");
    }

    if (principal == null) {
      log.warn("STOMP SUBSCRIBE 거부 — 인증되지 않은 사용자: destination={}", destination);
      throw new MessageDeliveryException("forbidden topic subscription");
    }

    String userEmail = principal.getName();
    User user = userRepository.findByUserEmail(userEmail).orElse(null);
    if (user == null || !user.getUserId().equals(requestedUserId)) {
      log.warn("STOMP SUBSCRIBE 거부 — 권한 없음: email={}, requestedUserId={}, destination={}",
              userEmail, requestedUserId, destination);
      throw new MessageDeliveryException("forbidden topic subscription");
    }
  }
}
