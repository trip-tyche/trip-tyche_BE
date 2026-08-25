package com.triptyche.backend.global.oauth.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RefreshTokenRepositoryTest {

  private static final String EMAIL = "user@triptyche.com";
  private static final String WEB_SESSION = "web-session-id";
  private static final String APP_SESSION = "app-session-id";
  private static final long TTL = 2_592_000L;

  @Mock
  private RedisTemplate<String, Object> redisTemplate;

  @Mock
  private ValueOperations<String, Object> valueOperations;

  @InjectMocks
  private RefreshTokenRepository refreshTokenRepository;

  @BeforeEach
  void setUp() {
    given(redisTemplate.opsForValue()).willReturn(valueOperations);
  }

  @Nested
  @DisplayName("웹·앱 동시 로그인")
  class ConcurrentSessions {

    @Test
    @DisplayName("세션이 다르면 서로 다른 키에 저장되어 덮어쓰지 않는다")
    void save_givenTwoSessions_writesSeparateKeys() {
      // given
      // when
      refreshTokenRepository.save(EMAIL, WEB_SESSION, "web-token", TTL);
      refreshTokenRepository.save(EMAIL, APP_SESSION, "app-token", TTL);

      // then
      verify(valueOperations).set("refresh_token:" + EMAIL + ":" + WEB_SESSION, "web-token",
              Duration.ofSeconds(TTL));
      verify(valueOperations).set("refresh_token:" + EMAIL + ":" + APP_SESSION, "app-token",
              Duration.ofSeconds(TTL));
    }

    @Test
    @DisplayName("한 세션을 삭제해도 다른 세션의 키는 삭제되지 않는다")
    void delete_givenOneSession_leavesOtherSessionKey() {
      // given
      given(redisTemplate.delete(any(String.class))).willReturn(true);

      // when
      refreshTokenRepository.delete(EMAIL, APP_SESSION);

      // then
      verify(redisTemplate).delete("refresh_token:" + EMAIL + ":" + APP_SESSION);
    }

    @Test
    @DisplayName("세션별로 저장된 토큰을 각각 조회한다")
    void find_givenSession_readsThatSessionKey() {
      // given
      given(valueOperations.get("refresh_token:" + EMAIL + ":" + WEB_SESSION)).willReturn("web-token");

      // when
      String found = refreshTokenRepository.find(EMAIL, WEB_SESSION);

      // then
      assertThat(found).isEqualTo("web-token");
    }
  }

  @Nested
  @DisplayName("회전 유예")
  class RotationGrace {

    @Test
    @DisplayName("expireIn은 해당 세션 키의 TTL만 줄인다")
    void expireIn_givenSession_shortensTtlOfThatKey() {
      // given
      given(redisTemplate.expire(any(String.class), any(Duration.class))).willReturn(true);

      // when
      boolean result = refreshTokenRepository.expireIn(EMAIL, WEB_SESSION, 10L);

      // then
      assertThat(result).isTrue();
      verify(redisTemplate).expire("refresh_token:" + EMAIL + ":" + WEB_SESSION, Duration.ofSeconds(10L));
    }
  }

  @Nested
  @DisplayName("조회 실패")
  class NotFound {

    @Test
    @DisplayName("저장된 값이 없으면 null을 반환한다")
    void find_givenMissingKey_returnsNull() {
      // given
      given(valueOperations.get(any(String.class))).willReturn(null);

      // when
      String found = refreshTokenRepository.find(EMAIL, WEB_SESSION);

      // then
      assertThat(found).isNull();
    }
  }
}
