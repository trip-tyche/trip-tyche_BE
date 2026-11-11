package com.triptyche.backend.global.util;

import com.triptyche.backend.global.common.ResultCode;
import com.triptyche.backend.global.exception.CustomException;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.security.Key;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtSecretKeyManager {

  @Value("${KAKAO_OAUTH_CLIENT_SECRET}")
  private String kakaoSecretKey;

  @Value("${GOOGLE_OAUTH_CLIENT_SECRET}")
  private String googleSecretKey;

  @Value("${GUEST_JWT_SECRET}")
  private String guestSecretKey;

  private Map<String, Key> secretKeyMap;

  @PostConstruct
  private void init() {
    secretKeyMap = Map.of(
        "kakao", Keys.hmacShaKeyFor(kakaoSecretKey.getBytes()),
        "google", Keys.hmacShaKeyFor(googleSecretKey.getBytes()),
        "guest", Keys.hmacShaKeyFor(guestSecretKey.getBytes())
    );
  }

  public Key getSecretKey(String provider) {
    return Optional.ofNullable(secretKeyMap.get(provider)).orElseThrow(() -> new CustomException(ResultCode.INVALID_PROVIDER));
  }
}
