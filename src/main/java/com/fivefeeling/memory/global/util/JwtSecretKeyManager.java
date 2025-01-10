package com.fivefeeling.memory.global.util;

import com.fivefeeling.memory.global.common.ResultCode;
import com.fivefeeling.memory.global.exception.CustomException;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.security.Key;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * JwtSecretKeyManager<br>
 * - JWT의 Secret Key를 제공자별로 관리하는 클래스.
 * - Secret Key는 HMAC SHA 알고리즘에 사용.
 * - 현재 지원하는 제공자: Kakao, Google
 */
@Component
@RequiredArgsConstructor
public class JwtSecretKeyManager {

  @Value("${KAKAO_OAUTH_CLIENT_SECRET}")
  private String kakaoSecretKey;

  @Value("${GOOGLE_OAUTH_CLIENT_SECRET}")
  private String googleSecretKey;

  private Map<String, Key> secretKeyMap;

  /**
   * Secret Key 초기화
   * - 애플리케이션 시작 시 제공자별 Secret Key를 Map에 저장
   */
  @PostConstruct
  private void init() {
    secretKeyMap = Map.of("kakao", Keys.hmacShaKeyFor(kakaoSecretKey.getBytes()), "google", Keys.hmacShaKeyFor(googleSecretKey.getBytes()));
  }

  /**
   * Secret Key 가져오기
   *
   * @param provider OAuth2 제공자 이름 ("kakao" 또는 "google")
   * @return 해당 제공자의 Secret Key
   * @throws CustomException 제공자 이름이 잘못된 경우
   */

  public Key getSecretKey(String provider) {
    return Optional.ofNullable(secretKeyMap.get(provider)).orElseThrow(() -> new CustomException(ResultCode.INVALID_PROVIDER));
  }
}
