package com.triptyche.backend.global.oauth.service;

import com.triptyche.backend.domain.user.model.UserRole;
import com.triptyche.backend.global.common.ResultCode;
import com.triptyche.backend.global.config.JwtProperties;
import com.triptyche.backend.global.exception.CustomException;
import com.triptyche.backend.global.oauth.dto.OneTimeCodePayload;
import com.triptyche.backend.global.oauth.dto.TokenIssueResponse;
import com.triptyche.backend.global.oauth.repository.OneTimeCodeRepository;
import com.triptyche.backend.global.oauth.repository.RefreshTokenRepository;
import com.triptyche.backend.global.util.JwtTokenProvider;
import com.triptyche.backend.global.util.SessionIdGenerator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenExchangeService {

  private final OneTimeCodeRepository oneTimeCodeRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final JwtTokenProvider jwtTokenProvider;
  private final JwtProperties jwtProperties;
  private final SessionIdGenerator sessionIdGenerator;

  public TokenIssueResponse exchange(String code) {
    OneTimeCodePayload payload = oneTimeCodeRepository.consume(code);

    if (payload == null) {
      log.warn("토큰 교환 거부 - 만료·미존재·이미 사용된 code");
      throw new CustomException(ResultCode.INVALID_AUTH_CODE);
    }

    String userEmail = payload.userEmail();
    String provider = payload.provider();
    String sessionId = sessionIdGenerator.generate();

    String accessToken = jwtTokenProvider.createAccessToken(
            userEmail, List.of(UserRole.USER.authority()), provider, sessionId);
    String refreshToken = jwtTokenProvider.createRefreshToken(userEmail, provider, sessionId);

    boolean saved = refreshTokenRepository.save(
            userEmail, sessionId, refreshToken, jwtProperties.refreshTokenExpirySeconds());

    if (!saved) {
      log.error("교환한 Refresh Token 저장 실패: user={}", userEmail);
      throw new CustomException(ResultCode.INTERNAL_SERVER_ERROR);
    }

    log.info("토큰 교환 성공: 사용자={}, 세션={}", userEmail, sessionId);
    return new TokenIssueResponse(accessToken, refreshToken, jwtProperties.accessTokenExpirySeconds());
  }
}
