package com.fivefeeling.memory.global.util;

import java.security.SecureRandom;

public final class TripKeyGenerator {

  private static final SecureRandom RANDOM = new SecureRandom();
  private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
  private static final int KEY_LENGTH = 6;

  private TripKeyGenerator() {
    // 인스턴스화 방지
  }

  /**
   * 6자리 랜덤 문자열을 생성합니다.
   *
   * @return 6자리 랜덤 문자열
   */
  public static String generateKey() {
    StringBuilder keyBuilder = new StringBuilder(KEY_LENGTH);
    for (int i = 0; i < KEY_LENGTH; i++) {
      keyBuilder.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
    }
    return keyBuilder.toString();
  }
}
