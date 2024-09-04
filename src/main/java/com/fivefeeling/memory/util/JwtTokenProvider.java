package com.fivefeeling.memory.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

  @Value("${spring.security.oauth2.client.registration.kakao.client-secret}")
  private String SECRET_KEY;

  // 토큰 생성
  public String createToken(String userEmail) {
    Claims claims = Jwts.claims().setSubject(userEmail);
    Date now = new Date();
    Date validity = new Date(now.getTime() + 3600000);

    // JWT 토큰 생성
    return Jwts.builder()
        // 서명을 하기 위한 키 설정
        .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
        .setClaims(claims)
        .setIssuedAt(now)
        .setExpiration(validity)
        .compact();
  }

  // 토큰 검증
  public boolean validateToken(String token) {
    try {
      Jwts.parser()
          .setSigningKey(SECRET_KEY)
          .parseClaimsJws(token);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  // 토큰에서 사용자 정보 추출
  public String getUserEmailFromToken(String token) {
    return Jwts.parser()
        .setSigningKey(SECRET_KEY)
        .parseClaimsJws(token)
        .getBody()
        .getSubject();
  }


}
