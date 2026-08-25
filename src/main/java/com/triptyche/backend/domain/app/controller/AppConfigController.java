package com.triptyche.backend.domain.app.controller;

import com.triptyche.backend.domain.app.dto.AppConfigResponse;
import com.triptyche.backend.global.common.RestResponse;
import com.triptyche.backend.global.config.AppConfigProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "10. 앱 설정 API")
@RestController
@RequestMapping("/v1/app")
@RequiredArgsConstructor
public class AppConfigController {

  private final AppConfigProperties appConfigProperties;

  @Operation(summary = "앱 버전 정책 조회 (강제 업데이트 판단용)")
  @GetMapping("/config")
  public RestResponse<AppConfigResponse> getAppConfig() {
    return RestResponse.success(new AppConfigResponse(
            appConfigProperties.minSupportedVersion(),
            appConfigProperties.latestVersion(),
            appConfigProperties.updateUrl()));
  }
}
