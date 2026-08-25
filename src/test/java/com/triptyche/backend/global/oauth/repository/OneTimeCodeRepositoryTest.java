package com.triptyche.backend.global.oauth.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.triptyche.backend.global.oauth.dto.OneTimeCodePayload;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OneTimeCodeRepositoryTest {

  private static final String EMAIL = "user@triptyche.com";
  private static final String PROVIDER = "kakao";
  private static final String STORED_JSON = "{\"userEmail\":\"user@triptyche.com\",\"provider\":\"kakao\"}";

  @Mock
  private RedisTemplate<String, Object> redisTemplate;

  @Mock
  private ValueOperations<String, Object> valueOperations;

  private OneTimeCodeRepository oneTimeCodeRepository;

  @BeforeEach
  void setUp() {
    given(redisTemplate.opsForValue()).willReturn(valueOperations);
    oneTimeCodeRepository = new OneTimeCodeRepository(redisTemplate, new ObjectMapper());
  }

  @Nested
  @DisplayName("code 발급")
  class Issue {

    @Test
    @DisplayName("oauth:code:{code} 키에 TTL 60초로 저장한다")
    void issue_givenPayload_storesWithSixtySecondTtl() {
      // given
      ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
      ArgumentCaptor<Object> valueCaptor = ArgumentCaptor.forClass(Object.class);

      // when
      String code = oneTimeCodeRepository.issue(new OneTimeCodePayload(EMAIL, PROVIDER));

      // then
      verify(valueOperations).set(keyCaptor.capture(), valueCaptor.capture(), eq(Duration.ofSeconds(60)));
      assertThat(keyCaptor.getValue()).isEqualTo("oauth:code:" + code);
      assertThat(valueCaptor.getValue()).isEqualTo(STORED_JSON);
    }

    @Test
    @DisplayName("발급된 code는 매번 다르고 128비트 이상이다")
    void issue_calledRepeatedly_returnsUnguessableDistinctCodes() {
      // given
      Set<String> codes = new HashSet<>();

      // when
      for (int i = 0; i < 500; i++) {
        codes.add(oneTimeCodeRepository.issue(new OneTimeCodePayload(EMAIL, PROVIDER)));
      }

      // then
      assertThat(codes).hasSize(500);
      assertThat(codes).allSatisfy(code -> assertThat(code.length()).isGreaterThanOrEqualTo(22));
    }
  }

  @Nested
  @DisplayName("code 소비")
  class Consume {

    @Test
    @DisplayName("저장된 payload를 돌려준다")
    void consume_givenStoredCode_returnsPayload() {
      // given
      given(valueOperations.getAndDelete("oauth:code:CODE")).willReturn(STORED_JSON);

      // when
      OneTimeCodePayload payload = oneTimeCodeRepository.consume("CODE");

      // then
      assertThat(payload).isEqualTo(new OneTimeCodePayload(EMAIL, PROVIDER));
    }

    @Test
    @DisplayName("같은 code를 두 번 소비하면 두 번째는 null이다")
    void consume_givenAlreadyUsedCode_returnsNull() {
      // given
      given(valueOperations.getAndDelete("oauth:code:CODE")).willReturn(STORED_JSON, (Object) null);

      // when
      OneTimeCodePayload first = oneTimeCodeRepository.consume("CODE");
      OneTimeCodePayload second = oneTimeCodeRepository.consume("CODE");

      // then
      assertThat(first).isNotNull();
      assertThat(second).isNull();
    }

    @Test
    @DisplayName("TTL이 지나 사라졌거나 존재하지 않는 code면 null이다")
    void consume_givenExpiredOrUnknownCode_returnsNull() {
      // given
      given(valueOperations.getAndDelete(anyString())).willReturn(null);

      // when
      OneTimeCodePayload payload = oneTimeCodeRepository.consume("EXPIRED");

      // then
      assertThat(payload).isNull();
    }

    @Test
    @DisplayName("code가 비어 있으면 Redis를 조회하지 않는다")
    void consume_givenBlankCode_skipsRedis() {
      // when
      OneTimeCodePayload payload = oneTimeCodeRepository.consume("  ");

      // then
      assertThat(payload).isNull();
      verify(valueOperations, never()).getAndDelete(anyString());
    }
  }
}
