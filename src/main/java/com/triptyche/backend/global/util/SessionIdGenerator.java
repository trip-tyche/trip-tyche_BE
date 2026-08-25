package com.triptyche.backend.global.util;

import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;

// 같은 값이 refresh 토큰의 jti와 access 토큰의 sid에 함께 실린다.
@Component
public class SessionIdGenerator {

  private static final int BYTE_LENGTH = 16;

  private final SecureRandom secureRandom = new SecureRandom();
  private final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

  public String generate() {
    byte[] bytes = new byte[BYTE_LENGTH];
    secureRandom.nextBytes(bytes);
    return encoder.encodeToString(bytes);
  }
}
