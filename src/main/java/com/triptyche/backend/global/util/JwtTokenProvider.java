package com.triptyche.backend.global.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.triptyche.backend.global.common.ResultCode;
import com.triptyche.backend.global.exception.CustomException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenProvider {
  private final JwtSecretKeyManager jwtSecretKeyManager;
  private final ObjectMapper objectMapper;
  private final AccessTokenBuilder accessTokenBuilder;
  private final RefreshTokenBuilder refreshTokenBuilder;
  private final GuestTokenBuilder guestTokenBuilder;
  private final SessionIdGenerator sessionIdGenerator;

  public String createAccessToken(String userEmail, List<String> roles, String provider) {
    return accessTokenBuilder.build(userEmail, roles, provider);
  }
  public String createAccessToken(String userEmail, List<String> roles, String provider, String sessionId) {
    return accessTokenBuilder.build(userEmail, roles, provider, sessionId);
  }
  public String createRefreshToken(String userEmail, String provider) {
    return refreshTokenBuilder.build(userEmail, provider, sessionIdGenerator.generate());
  }
  public String createRefreshToken(String userEmail, String provider, String sessionId) {
    return refreshTokenBuilder.build(userEmail, provider, sessionId);
  }
  public String createGuestToken(String userEmail, String provider) {
    return guestTokenBuilder.build(userEmail, provider);
  }

  // 여기서 잡는 예외는 모두 클라이언트가 보낸 토큰의 문제(만료·변조·형식 오류)이므로 warn으로 남긴다.
  // error로 남기면 Sentry 이벤트와 5xx 알림이 되어 실제 서버 장애와 구분되지 않는다.
  public void validateToken(String token) {
    try {
      String provider = extractProviderFromToken(token);
      Jwts.parserBuilder().setSigningKey(jwtSecretKeyManager.getSecretKey(provider)).build().parseClaimsJws(token);
    } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
      log.warn("JWT 서명 오류 또는 변조된 토큰: {}", e.getMessage());
      throw new CustomException(ResultCode.INVALID_JWT);
    } catch (ExpiredJwtException e) {
      log.warn("토큰 만료: {}", e.getMessage());
      throw new CustomException(ResultCode.EXPIRED_JWT);
    } catch (UnsupportedJwtException e) {
      log.warn("지원되지 않는 JWT 토큰: {}", e.getMessage());
      throw new CustomException(ResultCode.INVALID_JWT);
    } catch (IllegalArgumentException e) {
      log.warn("잘못된 JWT 토큰: {}", e.getMessage());
      throw new CustomException(ResultCode.INVALID_JWT);
    } catch (Exception e) {
      log.warn("JWT 파싱 오류 발생: {}", e.getMessage());
      throw new CustomException(ResultCode.JWT_PARSING_ERROR);
    }
  }

  public String getUserEmailFromToken(String token) {
    try {
      String provider = extractProviderFromToken(token);
      Key secretKey = jwtSecretKeyManager.getSecretKey(provider);
      return Jwts.parserBuilder().setSigningKey(secretKey).build()
              .parseClaimsJws(token).getBody().getSubject();
    } catch (Exception e) {
      log.error("JWT에서 사용자 이메일 추출 중 오류 발생: {}", e.getMessage());
      throw new CustomException(ResultCode.JWT_CLAIM_ERROR);
    }
  }
  public List<GrantedAuthority> getAuthorities(String token) {
    try {
      String provider = extractProviderFromToken(token);
      Key secretKey = jwtSecretKeyManager.getSecretKey(provider);
      Claims claims = Jwts.parserBuilder().setSigningKey(secretKey).build()
              .parseClaimsJws(token).getBody();
      List<String> roles = (List<String>) claims.get("roles");
      return roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
    } catch (Exception e) {
      throw new CustomException(ResultCode.JWT_CLAIM_ERROR);
    }
  }
  public String extractSessionId(String token) {
    try {
      String provider = extractProviderFromToken(token);
      Key secretKey = jwtSecretKeyManager.getSecretKey(provider);
      Claims claims = Jwts.parserBuilder().setSigningKey(secretKey).build()
              .parseClaimsJws(token).getBody();

      if (claims.getId() != null) {
        return claims.getId();
      }
      Object sid = claims.get("sid");
      return sid != null ? sid.toString() : null;
    } catch (CustomException e) {
      throw e;
    } catch (Exception e) {
      log.error("JWT에서 세션 식별자 추출 중 오류 발생: {}", e.getMessage());
      throw new CustomException(ResultCode.JWT_CLAIM_ERROR);
    }
  }

  public String extractProviderFromToken(String token) {
    try {
      String[] chunks = token.split("\\.");
      if (chunks.length != 3) {
        throw new CustomException(ResultCode.INVALID_JWT);
      }
      String payload = new String(Base64.getDecoder().decode(chunks[1]), StandardCharsets.UTF_8);
      return objectMapper.readTree(payload).get("provider").asText();
    } catch (Exception e) {
      log.warn("JWT에서 provider 추출 중 오류 발생: {}", e.getMessage());
      throw new CustomException(ResultCode.JWT_PARSING_ERROR);
    }
  }
}
