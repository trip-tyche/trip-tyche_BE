package com.triptyche.backend.global.util;

import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;

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
