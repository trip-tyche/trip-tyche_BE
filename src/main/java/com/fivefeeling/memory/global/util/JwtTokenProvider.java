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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
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

//  private final Key SECRET_KEY;

  @Value("${KAKAO_OAUTH_CLIENT_SECRET}")
  private String kakaoSecretKey;

  @Value("${GOOGLE_OAUTH_CLIENT_SECRET}")
  private String googleSecretKey;

  private Key KAKAO_SECRET_KEY;
  private Key GOOGLE_SECRET_KEY;

  @PostConstruct
  private void init() {
    try {
      String kakaoBase64Key = Base64.getEncoder().encodeToString(kakaoSecretKey.getBytes());
      this.KAKAO_SECRET_KEY = Keys.hmacShaKeyFor(Base64.getDecoder().decode(kakaoBase64Key));

      String googleBase64Key = Base64.getEncoder().encodeToString(googleSecretKey.getBytes());
      this.GOOGLE_SECRET_KEY = Keys.hmacShaKeyFor(Base64.getDecoder().decode(googleBase64Key));
    } catch (Exception e) {
      System.err.println("Error initializing JWT keys: " + e.getMessage());
      throw e;
    }
  }

  /*
    // 키를 Base64 인코딩된 상태로 가져와 HMAC 키로 변환
    public JwtTokenProvider(@Value("${spring.security.oauth2.client.registration.kakao.client-secret}") String secretKey) {
      String base64Key = Base64.getEncoder().encodeToString(secretKey.getBytes()); // Base64로 인코딩된 키 사용
      this.SECRET_KEY = Keys.hmacShaKeyFor(base64Key.getBytes());
    }

    // 토큰 생성
    public String createToken(String userEmail, List<String> roles) {
      Claims claims = Jwts.claims().setSubject(userEmail);
      claims.put("roles", roles);

      Date now = new Date();
      Date validity = new Date(now.getTime() + 3600000); // 1시간 유효기간

      return Jwts.builder()
          .setClaims(claims)
          .setIssuedAt(now)
          .setExpiration(validity)
          .signWith(SECRET_KEY, SignatureAlgorithm.HS256)
          .compact();
    }*/
  public String createToken(String userEmail, List<String> roles, String provider) {
    Claims claims = Jwts.claims().setSubject(userEmail);
    claims.put("roles", roles);
    claims.put("provider", provider);

    Date now = new Date();
    Date validity = new Date(now.getTime() + 3600000); // 1시간 유효기간

    Key secretKey = provider.equals("kakao") ? KAKAO_SECRET_KEY : GOOGLE_SECRET_KEY;

    return Jwts.builder()
        .setClaims(claims)
        .setIssuedAt(now)
        .setExpiration(validity)
        .signWith(secretKey, SignatureAlgorithm.HS256)
        .compact();
  }

  // 토큰 검증
  public boolean validateToken(String token) {
    try {
      Key secretKey = getSecretKeyFromToken(token);
      Claims claims = (Claims) Jwts.parserBuilder()
          .setSigningKey(secretKey)
          .build()
          .parseClaimsJws(token);
      log.debug("제공된 토큰: {}", claims.get("provider"));
      return true;
    } catch (SecurityException | MalformedJwtException e) {
      // 서명 오류 또는 토큰 변조
      log.error("JWT 서명 오류 또는 변조된 토큰: {}", e.getMessage());
      throw new CustomException(ResultCode.INVALID_JWT);
    } catch (ExpiredJwtException e) {
      // 토큰 만료
      log.error("토큰 만료: {}", e.getMessage()); // 로그 추가
      throw new CustomException(ResultCode.EXPIRED_JWT);
    } catch (UnsupportedJwtException e) {
      // 지원하지 않는 토큰
      log.error("지원되지 않는 JWT 토큰: {}", e.getMessage());
      throw new CustomException(ResultCode.INVALID_JWT);
    } catch (IllegalArgumentException e) {
      // 잘못된 토큰
      log.error("잘못된 JWT 토큰: {}", e.getMessage());
      throw new CustomException(ResultCode.INVALID_JWT);
    } catch (Exception e) {
      // 기타 예외
      log.error("JWT 파싱 오류 발생: {}", e.getMessage());
      throw new CustomException(ResultCode.JWT_PARSING_ERROR);
    }
  }

  // provider에 따라 Secret Key 가져오기
  private Key getSecretKeyFromToken(String token) {
// 토큰을 '.'으로 분리
    String[] chunks = token.split("\\.");
    if (chunks.length != 3) {
      log.error("유효하지 않은 JWT 토큰 형식입니다.");
      throw new CustomException(ResultCode.INVALID_JWT);
    }

    // 페이로드 부분(Base64 디코딩)
    String payload = new String(Base64.getDecoder().decode(chunks[1]), StandardCharsets.UTF_8);

    // JSON 파싱하여 Claims 생성
    ObjectMapper objectMapper = new ObjectMapper();
    Map<String, Object> claims;
    try {
      claims = objectMapper.readValue(payload, Map.class);
    } catch (IOException e) {
      log.error("JWT 페이로드 파싱 오류: {}", e.getMessage());
      throw new CustomException(ResultCode.JWT_CLAIM_ERROR);
    }

    String provider = (String) claims.get("provider");
    if (provider == null) {
      log.error("JWT에 'provider' 정보가 없습니다.");
      throw new CustomException(ResultCode.JWT_CLAIM_ERROR);
    }

    return provider.equals("kakao") ? KAKAO_SECRET_KEY : GOOGLE_SECRET_KEY;
  }

  // 토큰에서 사용자 정보 추출
  public String getUserEmailFromToken(String token) {
    try {
      Key secretKey = getSecretKeyFromToken(token);
      return Jwts.parserBuilder()
          .setSigningKey(secretKey)
          .build()
          .parseClaimsJws(token)
          .getBody()
          .getSubject();
    } catch (Exception e) {
      log.error("JWT에서 사용자 이메일 추출 중 오류 발생: {}", e.getMessage());
      throw new CustomException(ResultCode.JWT_CLAIM_ERROR);
    }
  }

  // 토큰에서 권한 정보 추출
  public List<GrantedAuthority> getAuthorities(String token) {
    try {
      Key secretKey = getSecretKeyFromToken(token);
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
}