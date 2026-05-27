package com.triptyche.backend.global.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.triptyche.backend.domain.user.model.User;
import com.triptyche.backend.domain.user.repository.UserRepository;
import java.security.Principal;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

@ExtendWith(MockitoExtension.class)
class StompTopicAuthInterceptorTest {

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private StompTopicAuthInterceptor interceptor;

  private Message<?> buildMessage(StompCommand command, String destination, Principal principal) {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
    accessor.setDestination(destination);
    accessor.setUser(principal);
    accessor.setSessionId("test-session");
    return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
  }

  private Principal principalOf(String name) {
    return () -> name;
  }

  @Nested
  @DisplayName("SUBSCRIBE /topic/media-processed/{userId}")
  class MediaProcessedSubscription {

    @Test
    @DisplayName("principal name의 email이 userId와 일치하면 통과한다")
    void subscribe_givenMatchingUser_passes() {
      // given
      long userId = 42L;
      String email = "user@example.com";
      User user = User.builder()
              .userId(userId)
              .userName("testUser")
              .userEmail(email)
              .provider("google")
              .build();
      given(userRepository.findByUserEmail(email)).willReturn(Optional.of(user));
      Message<?> message = buildMessage(StompCommand.SUBSCRIBE, "/topic/media-processed/42", principalOf(email));

      // when
      Message<?> result = interceptor.preSend(message, null);

      // then
      assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("principal의 email이 다른 userId를 가지면 MessageDeliveryException을 던진다")
    void subscribe_givenMismatchedUserId_throwsException() {
      // given
      String email = "other@example.com";
      User user = User.builder()
              .userId(99L)
              .userName("otherUser")
              .userEmail(email)
              .provider("google")
              .build();
      given(userRepository.findByUserEmail(email)).willReturn(Optional.of(user));
      Message<?> message = buildMessage(StompCommand.SUBSCRIBE, "/topic/media-processed/42", principalOf(email));

      // when & then
      assertThatThrownBy(() -> interceptor.preSend(message, null))
              .isInstanceOf(MessageDeliveryException.class)
              .hasMessage("forbidden topic subscription");
    }

    @Test
    @DisplayName("destination의 userId가 숫자가 아니면 MessageDeliveryException을 던진다")
    void subscribe_givenNonNumericUserId_throwsException() {
      // given
      Message<?> message = buildMessage(StompCommand.SUBSCRIBE, "/topic/media-processed/abc", principalOf("user@example.com"));

      // when & then
      assertThatThrownBy(() -> interceptor.preSend(message, null))
              .isInstanceOf(MessageDeliveryException.class)
              .hasMessage("invalid topic");
    }

    @Test
    @DisplayName("principal이 null이면 MessageDeliveryException을 던진다")
    void subscribe_givenNullPrincipal_throwsException() {
      // given
      Message<?> message = buildMessage(StompCommand.SUBSCRIBE, "/topic/media-processed/42", null);

      // when & then
      assertThatThrownBy(() -> interceptor.preSend(message, null))
              .isInstanceOf(MessageDeliveryException.class)
              .hasMessage("forbidden topic subscription");
    }
  }

  @Nested
  @DisplayName("다른 토픽 또는 다른 커맨드는 검증 없이 통과한다")
  class NonMediaProcessedTopics {

    @Test
    @DisplayName("SUBSCRIBE /topic/share-notifications/{userId} — prefix가 달라 검증 생략, 통과")
    void subscribe_givenShareNotificationsTopic_passes() {
      // given — principal/userRepository 설정 없이도 통과해야 함
      Message<?> message = buildMessage(StompCommand.SUBSCRIBE, "/topic/share-notifications/42", principalOf("user@example.com"));

      // when
      Message<?> result = interceptor.preSend(message, null);

      // then
      assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("SEND 커맨드는 검증 없이 통과한다")
    void send_command_passes() {
      // given
      Message<?> message = buildMessage(StompCommand.SEND, "/topic/media-processed/42", principalOf("user@example.com"));

      // when
      Message<?> result = interceptor.preSend(message, null);

      // then
      assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("CONNECT 커맨드는 검증 없이 통과한다")
    void connect_command_passes() {
      // given
      Message<?> message = buildMessage(StompCommand.CONNECT, null, principalOf("user@example.com"));

      // when
      Message<?> result = interceptor.preSend(message, null);

      // then
      assertThat(result).isNotNull();
    }
  }
}
