package com.triptyche.backend.global.oauth.controller;

import com.triptyche.backend.global.common.RestResponse;
import com.triptyche.backend.global.oauth.dto.TestTokenCreateRequest;
import com.triptyche.backend.global.oauth.dto.TestTokenCreateResponse;
import com.triptyche.backend.global.util.JwtTokenProvider;
import com.triptyche.backend.domain.user.model.UserRole;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("local")
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class TestTokenController {

  private static final String DEFAULT_PROVIDER = "google";

  private final JwtTokenProvider jwtTokenProvider;

  @PostMapping("/test-token")
  public RestResponse<TestTokenCreateResponse> createTestToken(@RequestBody TestTokenCreateRequest request) {
    String provider = request.provider() != null ? request.provider() : DEFAULT_PROVIDER;
    String accessToken = jwtTokenProvider.createAccessToken(request.email(), List.of(UserRole.USER.authority()), provider);
    return RestResponse.success(new TestTokenCreateResponse(accessToken));
  }
}
