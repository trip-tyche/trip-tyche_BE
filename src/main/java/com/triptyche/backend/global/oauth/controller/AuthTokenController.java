package com.triptyche.backend.global.oauth.controller;

import com.triptyche.backend.global.common.RestResponse;
import com.triptyche.backend.global.oauth.dto.TokenExchangeRequest;
import com.triptyche.backend.global.oauth.dto.TokenIssueResponse;
import com.triptyche.backend.global.oauth.service.TokenExchangeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 앱 전용 경로. 웹이 쓰는 /v1/auth/refresh 는 그대로 둔다.
@Tag(name = "0. 로그인&인증관련 API")
@RestController
@RequestMapping("/v1/auth/token")
@RequiredArgsConstructor
public class AuthTokenController {

  private final TokenExchangeService tokenExchangeService;

  @Operation(summary = "1회용 code를 토큰으로 교환 (앱)")
  @PostMapping("/exchange")
  public RestResponse<TokenIssueResponse> exchange(@RequestBody TokenExchangeRequest request) {
    return RestResponse.success(tokenExchangeService.exchange(request.code()));
  }
}
