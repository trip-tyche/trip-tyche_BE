package com.fivefeeling.memory.global.oauth.repository;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepository {

  private final RedisTemplate<String, Object> redisTemplate;
  private static final String KEY_PREFIX = "refresh_token:";


  /**
   * 사용자 이메일을 키로 refresh 토큰을 Redis에 저장합니다.
   *
   * @param userEmail
   *         사용자 이메일 (토큰 저장 시 고유 식별자로 사용)
   * @param refreshToken
   *         발급된 refresh 토큰
   * @param expirationSeconds
   *         토큰 만료 시간(초 단위, 예: 2592000초 = 30일)
   */
  public void save(String userEmail, String refreshToken, long expirationSeconds) {
    String key = KEY_PREFIX + userEmail;
    redisTemplate.opsForValue().set(key, refreshToken, Duration.ofSeconds(expirationSeconds));
  }

  /**
   * 사용자 이메일에 해당하는 refresh 토큰을 Redis에서 조회합니다.
   *
   * @param userEmail
   *         사용자 이메일
   * @return Redis에 저장된 refresh 토큰, 없으면 null 반환
   */
  public String findByUserEmail(String userEmail) {
    String key = KEY_PREFIX + userEmail;
    Object token = redisTemplate.opsForValue().get(key);
    return token != null ? token.toString() : null;
  }

  /**
   * 사용자 이메일에 해당하는 refresh 토큰을 Redis에서 삭제합니다.
   *
   * @param userEmail
   *         사용자 이메일
   */
  public void delete(String userEmail) {
    String key = KEY_PREFIX + userEmail;
    redisTemplate.delete(key);
  }
}
