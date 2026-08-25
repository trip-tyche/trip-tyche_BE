package com.triptyche.backend.global.util;

import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;

/**
 * 로그인 세션 하나를 식별하는 난수 생성기.
 * <p>
 * 같은 값이 refresh 토큰의 {@code jti}와 access 토큰의 {@code sid}에 함께 실려,
 * 둘 중 무엇을 들고 와도 어느 세션인지 특정할 수 있게 한다.
 */
@Component
public class SessionIdGenerator {

  private static final int BYTE_LENGTH = 16; // 128비트

  private final SecureRandom secureRandom = new SecureRandom();
  private final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

  public String generate() {
    byte[] bytes = new byte[BYTE_LENGTH];
    secureRandom.nextBytes(bytes);
    return encoder.encodeToString(bytes);
  }
}
