package com.triptyche.backend.global.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.triptyche.backend.global.common.ResultCode;
import com.triptyche.backend.global.exception.CustomException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

/**
 * JwtTokenProvider<br>
 * - JWT 생성, 검증, 파싱을 담당하는 클래스<br>
 * - Secret Key는 JwtSecretKeyManager를 통해 관리<br>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenProvider {

  private final JwtSecretKeyManager jwtSecretKeyManager;

  /**
   * JWT 토큰 생성
   *
   * @param userEmail
   *         사용자 이메일
   * @param roles
   *         사용자 권한 목록
   * @param provider
   *         OAuth2 제공자 이름(ex: google, kakao 등)
   * @return 생성된 JWT 토큰
   */
  public String createAccessToken(String userEmail, List<String> roles, String provider) {
    Claims claims = Jwts.claims().setSubject(userEmail);
    claims.put("roles", roles);
    claims.put("provider", provider);

    Date now = new Date();
    Date validity = new Date(now.getTime() + 3600000); // 1시간

    return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(now)
            .setExpiration(validity)
            .signWith(jwtSecretKeyManager.getSecretKey(provider), SignatureAlgorithm.HS256)
            .compact();
  }

  /**
   * Refresh Token 생성
   *
   * @param userEmail
   *         사용자 이메일 (Refresh Token 생성 시 사용자 식별 용도로 사용)
   * @param provider
   *         OAuth2 제공자 이름
   * @return 생성된 Refresh Token (만료시간 30일)
   */
  public String createRefreshToken(String userEmail, String provider) {
    Claims claims = Jwts.claims().setSubject(userEmail);
    claims.put("provider", provider);

    Date now = new Date();
    Date validity = new Date(now.getTime() + 2592000000L); // 30일

    return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(now)
            .setExpiration(validity)
            .signWith(jwtSecretKeyManager.getSecretKey(provider), SignatureAlgorithm.HS256)
            .compact();
  }

  /**
   * JWT 유효성 검증
   *
   * @param token
   *         클라이언트에서 전달받은 JWT
   * @throws CustomException
   *         검증 실패 시 적절한 예외를 던진다.
   */
  public void validateToken(String token) {
    try {
      // Step 1: 토큰에서 provider 추출
      String provider = extractProviderFromToken(token);
      log.debug("추출된 provider: {}", provider);

      // Step 2: 할당된 키로 서명 검증
      Jwts.parserBuilder().setSigningKey(jwtSecretKeyManager.getSecretKey(provider)).build().parseClaimsJws(token);

      log.debug("토큰 검증 성공. provider: {}", provider);
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

  /**
   * JWT에서 사용자 이메일 추출
   *
   * @param token
   *         클라이언트에서 전달받은 JWT
   * @return JWT에 저장된 사용자 이메일
   */
  public String getUserEmailFromToken(String token) {
    try {
      String provider = extractProviderFromToken(token);
      Key secretKey = jwtSecretKeyManager.getSecretKey(provider);

      String userEmail = Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token).getBody()
              .getSubject();

      log.debug("추출된 사용자 이메일: {}", userEmail);
      return userEmail;
    } catch (Exception e) {
      log.error("JWT에서 사용자 이메일 추출 중 오류 발생: {}", e.getMessage());
      throw new CustomException(ResultCode.JWT_CLAIM_ERROR);
    }
  }

  /**
   * JWT에서 사용자 권한 목록 추출
   *
   * @param token
   *         클라이언트에서 전달받은 JWT
   * @return JWT에 저장된 사용자 권한 목록
   */
  public List<GrantedAuthority> getAuthorities(String token) {
    try {
      String provider = extractProviderFromToken(token);
      Key secretKey = jwtSecretKeyManager.getSecretKey(provider);

      Claims claims = Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token).getBody();

      List<String> roles = (List<String>) claims.get("roles");
      return roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
    } catch (Exception e) {
      throw new CustomException(ResultCode.JWT_CLAIM_ERROR);
    }
  }

  /**
   * JWT에서 제공자(provider) 정보 추출
   *
   * @param token
   *         클라이언트에서 전달받은 JWT
   * @return JWT에 저장된 제공자(provider) 정보
   * @throws CustomException
   *         JWT 형식 오류나 파싱 오류 발생 시
   */
  public String extractProviderFromToken(String token) {
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
}
