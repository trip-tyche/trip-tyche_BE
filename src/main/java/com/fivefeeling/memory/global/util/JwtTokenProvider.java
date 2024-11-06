package com.fivefeeling.memory.global.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fivefeeling.memory.global.common.ResultCode;
import com.fivefeeling.memory.global.exception.CustomException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenProvider {

  @Value("${KAKAO_OAUTH_CLIENT_SECRET}")
  private String kakaoSecretKey;

  @Value("${GOOGLE_OAUTH_CLIENT_SECRET}")
  private String googleSecretKey;

  private Key KAKAO_SECRET_KEY;
  private Key GOOGLE_SECRET_KEY;

  @PostConstruct
  private void init() {
    try {
      this.KAKAO_SECRET_KEY = Keys.hmacShaKeyFor(kakaoSecretKey.getBytes());
      this.GOOGLE_SECRET_KEY = Keys.hmacShaKeyFor(googleSecretKey.getBytes());
    } catch (Exception e) {
      System.err.println("Error initializing JWT keys: " + e.getMessage());
      throw e;
    }
  }

  public String createToken(String userEmail, List<String> roles, String provider) {
    Claims claims = Jwts.claims().setSubject(userEmail);
    claims.put("roles", roles);
    claims.put("provider", provider);

    Date now = new Date();
    Date validity = new Date(now.getTime() + 3600000);

    Key secretKey;
    if ("kakao".equals(provider)) {
      secretKey = KAKAO_SECRET_KEY;
    } else if ("google".equals(provider)) {
      secretKey = GOOGLE_SECRET_KEY;
    } else {
      throw new CustomException(ResultCode.INVALID_PROVIDER);
    }

    return Jwts.builder()
        .setClaims(claims)
        .setIssuedAt(now)
        .setExpiration(validity)
        .signWith(secretKey, SignatureAlgorithm.HS256)
        .compact();
  }

  public boolean validateToken(String token) {
    try {
      // Step 1: 토큰에서 provider 추출
      String provider = extractProviderFromToken(token);
      log.debug("추출된 provider: {}", provider);

      // Step 2: provider에 맞는 secretKey 할당
      Key secretKey = getSecretKeyByProvider(provider);

      // Step 3: 할당된 키로 서명 검증
      Jwts.parserBuilder()
          .setSigningKey(secretKey)
          .build()
          .parseClaimsJws(token);

      log.debug("토큰 검증 성공. provider: {}", provider);
      return true;
    } catch (SecurityException | MalformedJwtException e) {
      log.error("JWT 서명 오류 또는 변조된 토큰: {}", e.getMessage());
      throw new CustomException(ResultCode.INVALID_JWT);
    } catch (ExpiredJwtException e) {
      log.error("토큰 만료: {}", e.getMessage());
      throw new CustomException(ResultCode.EXPIRED_JWT);
    } catch (UnsupportedJwtException e) {
      log.error("지원되지 않는 JWT 토큰: {}", e.getMessage());
      throw new CustomException(ResultCode.INVALID_JWT);
    } catch (IllegalArgumentException e) {
      log.error("잘못된 JWT 토큰: {}", e.getMessage());
      throw new CustomException(ResultCode.INVALID_JWT);
    } catch (Exception e) {
      log.error("JWT 파싱 오류 발생: {}", e.getMessage());
      throw new CustomException(ResultCode.JWT_PARSING_ERROR);
    }
  }

  public String getUserEmailFromToken(String token) {
    try {
      String provider = extractProviderFromToken(token);
      Key secretKey = getSecretKeyByProvider(provider);

      String userEmail = Jwts.parserBuilder()
          .setSigningKey(secretKey)
          .build()
          .parseClaimsJws(token)
          .getBody()
          .getSubject();

      log.debug("추출된 사용자 이메일: {}", userEmail);
      return userEmail;
    } catch (Exception e) {
      log.error("JWT에서 사용자 이메일 추출 중 오류 발생: {}", e.getMessage());
      throw new CustomException(ResultCode.JWT_CLAIM_ERROR);
    }
  }

  public List<GrantedAuthority> getAuthorities(String token) {
    try {
      String provider = extractProviderFromToken(token);
      Key secretKey = getSecretKeyByProvider(provider);

      Claims claims = Jwts.parserBuilder()
          .setSigningKey(secretKey)
          .build()
          .parseClaimsJws(token)
          .getBody();

      List<String> roles = (List<String>) claims.get("roles");
      return roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
    } catch (Exception e) {
      throw new CustomException(ResultCode.JWT_CLAIM_ERROR);
    }
  }

  private String extractProviderFromToken(String token) {
    try {
      String[] chunks = token.split("\\.");
      if (chunks.length != 3) {
        log.error("유효하지 않은 JWT 토큰 형식입니다.");
        throw new CustomException(ResultCode.INVALID_JWT);
      }

      String payload = new String(Base64.getDecoder().decode(chunks[1]), StandardCharsets.UTF_8);
      String provider = new ObjectMapper().readTree(payload).get("provider").asText();
      log.debug("디코딩된 페이로드에서 추출된 provider: {}", provider);
      return provider;
    } catch (Exception e) {
      log.error("JWT에서 provider 추출 중 오류 발생: {}", e.getMessage());
      throw new CustomException(ResultCode.JWT_PARSING_ERROR);
    }
  }

  private Key getSecretKeyByProvider(String provider) {
    if ("kakao".equals(provider)) {
      return KAKAO_SECRET_KEY;
    } else if ("google".equals(provider)) {
      return GOOGLE_SECRET_KEY;
    } else {
      log.error("알 수 없는 provider: {}", provider);
      throw new CustomException(ResultCode.INVALID_PROVIDER);
    }
  }
}
