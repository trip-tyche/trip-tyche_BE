package com.triptyche.backend.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.SendResponse;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FcmSenderTest {

  private final FcmSender fcmSender = new FcmSender();

  private SendResponse success() {
    SendResponse response = mock(SendResponse.class);
    given(response.isSuccessful()).willReturn(true);
    return response;
  }

  private SendResponse failure(MessagingErrorCode code) {
    FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
    given(exception.getMessagingErrorCode()).willReturn(code);

    SendResponse response = mock(SendResponse.class);
    given(response.isSuccessful()).willReturn(false);
    given(response.getException()).willReturn(exception);
    return response;
  }

  private BatchResponse batch(List<SendResponse> responses) {
    int failureCount = (int) responses.stream().filter(response -> !response.isSuccessful()).count();

    BatchResponse batch = mock(BatchResponse.class);
    given(batch.getResponses()).willReturn(responses);
    given(batch.getFailureCount()).willReturn(failureCount);
    return batch;
  }

  @Nested
  @DisplayName("무효 토큰 분류")
  class Classification {

    @Test
    @DisplayName("UNREGISTERED 토큰은 삭제 대상이다")
    void invalidTokens_givenUnregistered_marksForDeletion() {
      // given
      List<String> tokens = List.of("LIVE", "DEAD");
      BatchResponse response = batch(List.of(success(), failure(MessagingErrorCode.UNREGISTERED)));

      // when
      List<String> invalid = fcmSender.invalidTokens(tokens, response);

      // then
      assertThat(invalid).containsExactly("DEAD");
    }

    @Test
    @DisplayName("일시적 오류는 삭제 대상이 아니다")
    void invalidTokens_givenTransientError_keepsToken() {
      // given
      List<String> tokens = List.of("LIVE", "BUSY");
      BatchResponse response = batch(List.of(success(), failure(MessagingErrorCode.UNAVAILABLE)));

      // when
      List<String> invalid = fcmSender.invalidTokens(tokens, response);

      // then
      assertThat(invalid).isEmpty();
    }

    @Test
    @DisplayName("실패가 없으면 빈 목록이다")
    void invalidTokens_givenNoFailure_returnsEmpty() {
      // given
      BatchResponse response = batch(List.of(success(), success()));

      // when
      List<String> invalid = fcmSender.invalidTokens(List.of("A", "B"), response);

      // then
      assertThat(invalid).isEmpty();
    }
  }

  @Nested
  @DisplayName("메시지 payload 문제 방어")
  class PayloadFailureGuard {

    @Test
    @DisplayName("배치 전체가 INVALID_ARGUMENT면 토큰을 지우지 않는다")
    void invalidTokens_givenWholeBatchInvalidArgument_deletesNothing() {
      // given
      List<String> tokens = List.of("A", "B", "C");
      BatchResponse response = batch(List.of(
              failure(MessagingErrorCode.INVALID_ARGUMENT),
              failure(MessagingErrorCode.INVALID_ARGUMENT),
              failure(MessagingErrorCode.INVALID_ARGUMENT)));

      // when
      List<String> invalid = fcmSender.invalidTokens(tokens, response);

      // then
      assertThat(invalid).isEmpty();
    }

    @Test
    @DisplayName("일부만 INVALID_ARGUMENT면 그 토큰만 삭제한다")
    void invalidTokens_givenPartialInvalidArgument_deletesOnlyThose() {
      // given
      List<String> tokens = List.of("A", "B", "C");
      BatchResponse response = batch(List.of(
              success(),
              failure(MessagingErrorCode.INVALID_ARGUMENT),
              failure(MessagingErrorCode.UNREGISTERED)));

      // when
      List<String> invalid = fcmSender.invalidTokens(tokens, response);

      // then
      assertThat(invalid).containsExactly("B", "C");
    }

    @Test
    @DisplayName("기기가 하나뿐이면 payload 문제와 구분할 수 없어 삭제한다")
    void invalidTokens_givenSingleTokenInvalidArgument_stillDeletes() {
      // given
      BatchResponse response = batch(List.of(failure(MessagingErrorCode.INVALID_ARGUMENT)));

      // when
      List<String> invalid = fcmSender.invalidTokens(List.of("ONLY"), response);

      // then
      assertThat(invalid).containsExactly("ONLY");
    }
  }
}
