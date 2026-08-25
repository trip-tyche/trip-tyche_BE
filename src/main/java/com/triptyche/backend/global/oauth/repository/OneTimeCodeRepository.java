package com.triptyche.backend.global.oauth.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.triptyche.backend.global.oauth.dto.OneTimeCodePayload;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@Slf4j
public class OneTimeCodeRepository {

  private static final String KEY_PREFIX = "oauth:code:";
  private static final Duration TTL = Duration.ofSeconds(60);
  private static final int CODE_BYTE_LENGTH = 32;

  private final RedisTemplate<String, Object> redisTemplate;
  private final ObjectMapper objectMapper;

  private final SecureRandom secureRandom = new SecureRandom();
  private final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

  public String issue(OneTimeCodePayload payload) {
    String code = generateCode();
    try {
      redisTemplate.opsForValue()
              .set(KEY_PREFIX + code, objectMapper.writeValueAsString(payload), TTL);
      return code;
    } catch (Exception e) {
      log.error("1회용 code 발급 실패: user={}, error={}", payload.userEmail(), e.getMessage());
      return null;
    }
  }

  public OneTimeCodePayload consume(String code) {
    if (code == null || code.isBlank()) {
      return null;
    }

    try {
      Object stored = redisTemplate.opsForValue().getAndDelete(KEY_PREFIX + code);

      if (stored == null) {
        log.warn("1회용 code 소비 실패 (만료·미존재·이미 사용됨)");
        return null;
      }
      return objectMapper.readValue(stored.toString(), OneTimeCodePayload.class);
    } catch (Exception e) {
      log.error("1회용 code 소비 중 오류 발생: {}", e.getMessage());
      return null;
    }
  }

  private String generateCode() {
    byte[] bytes = new byte[CODE_BYTE_LENGTH];
    secureRandom.nextBytes(bytes);
    return encoder.encodeToString(bytes);
  }
}
