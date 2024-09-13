package com.fivefeeling.memory.global.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

  private final Key SECRET_KEY;

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
  }

  // 토큰 검증
  public boolean validateToken(String token) {
    try {
      Jwts.parserBuilder()
          .setSigningKey(SECRET_KEY)
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

  // 토큰에서 사용자 정보 추출
  public String getUserEmailFromToken(String token) {
    return Jwts.parserBuilder()
        .setSigningKey(SECRET_KEY)
        .build()
        .parseClaimsJws(token)
        .getBody()
        .getSubject();
  }

  // 토큰에서 권한 정보 추출
  public List<GrantedAuthority> getAuthorities(String token) {
    Claims claims = Jwts.parserBuilder()
        .setSigningKey(SECRET_KEY)
        .build()
        .parseClaimsJws(token)
        .getBody();

    List<String> roles = (List<String>) claims.get("roles");
    return roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
  }
}