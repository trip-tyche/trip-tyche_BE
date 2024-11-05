package com.fivefeeling.memory.global.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
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
      // 강력한 키를 자동 생성하여 256비트 이상의 HMAC-SHA 알고리즘에 적합한 키를 설정
      this.KAKAO_SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);
      this.GOOGLE_SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);
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
      Jwts.parserBuilder()
          .setSigningKey(secretKey)
          .build()
          .parseClaimsJws(token);
      return true;
    } catch (DecodingException e) {
      // 디코딩 오류 처리
      System.err.println("DecodingException: Invalid JWT format - " + e.getMessage());
      return false;
    } catch (Exception e) {
      // 일반적인 검증 오류 처리
      System.err.println("Token validation failed - " + e.getMessage());
      return false;
    }
  }

  // provider에 따라 Secret Key 가져오기
  private Key getSecretKeyFromToken(String token) {
    Claims claims = Jwts.parserBuilder()
        .build()
        .parseClaimsJws(token)
        .getBody();

    String provider = (String) claims.get("provider");
    return provider.equals("kakao") ? KAKAO_SECRET_KEY : GOOGLE_SECRET_KEY;
  }

  // 토큰에서 사용자 정보 추출
  public String getUserEmailFromToken(String token) {
    Key secretKey = getSecretKeyFromToken(token);
    return Jwts.parserBuilder()
        .setSigningKey(secretKey)
        .build()
        .parseClaimsJws(token)
        .getBody()
        .getSubject();
  }

  // 토큰에서 권한 정보 추출
  public List<GrantedAuthority> getAuthorities(String token) {
    Key secretKey = getSecretKeyFromToken(token);
    Claims claims = Jwts.parserBuilder()
        .setSigningKey(secretKey)
        .build()
        .parseClaimsJws(token)
        .getBody();

    List<String> roles = (List<String>) claims.get("roles");
    return roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
  }
}