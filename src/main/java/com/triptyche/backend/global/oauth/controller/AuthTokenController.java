package com.triptyche.backend.global.oauth.controller;

import com.triptyche.backend.global.common.RestResponse;
import com.triptyche.backend.global.oauth.dto.TokenExchangeRequest;
import com.triptyche.backend.global.oauth.dto.TokenIssueResponse;
import com.triptyche.backend.global.oauth.dto.TokenRefreshRequest;
import com.triptyche.backend.global.config.JwtProperties;
import com.triptyche.backend.global.oauth.service.TokenExchangeService;
import com.triptyche.backend.global.oauth.service.TokenRefreshService;
import java.util.Map;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "0. 로그인&인증관련 API")
@RestController
@RequestMapping("/v1/auth/token")
@RequiredArgsConstructor
public class AuthTokenController {

  private final TokenExchangeService tokenExchangeService;
  private final TokenRefreshService tokenRefreshService;
  private final JwtProperties jwtProperties;

  @Operation(summary = "1회용 code를 토큰으로 교환 (앱)")
  @PostMapping("/exchange")
  public RestResponse<TokenIssueResponse> exchange(@RequestBody TokenExchangeRequest request) {
    return RestResponse.success(tokenExchangeService.exchange(request.code()));
  }

  @Operation(summary = "refresh 토큰으로 access 토큰 갱신 (앱)")
  @PostMapping("/refresh")
  public RestResponse<TokenIssueResponse> refresh(@RequestBody TokenRefreshRequest request) {
    Map<String, String> tokens = tokenRefreshService.refreshToken(request.refreshToken());

    return RestResponse.success(new TokenIssueResponse(
            tokens.get("accessToken"),
            tokens.get("refreshToken"),
            jwtProperties.accessTokenExpirySeconds()));
  }
}
