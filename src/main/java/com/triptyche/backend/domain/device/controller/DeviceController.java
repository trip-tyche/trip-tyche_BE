package com.triptyche.backend.domain.device.controller;

import com.triptyche.backend.domain.device.dto.DeviceRegisterRequest;
import com.triptyche.backend.domain.device.service.DeviceService;
import com.triptyche.backend.domain.user.model.User;
import com.triptyche.backend.global.auth.CurrentUser;
import com.triptyche.backend.global.common.RestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "6. 푸시 알림 API")
@RestController
@RequestMapping("/v1/devices")
@RequiredArgsConstructor
public class DeviceController {

  private final DeviceService deviceService;

  @Operation(summary = "디바이스 토큰 등록")
  @PostMapping
  public RestResponse<String> register(@CurrentUser User user,
                                       @RequestBody DeviceRegisterRequest request) {
    deviceService.register(user.getUserId(), request);
    return RestResponse.success("디바이스가 등록되었습니다.");
  }

  @Operation(summary = "디바이스 토큰 해제")
  @DeleteMapping("/{token}")
  public RestResponse<String> unregister(@CurrentUser User user, @PathVariable String token) {
    deviceService.unregister(user.getUserId(), token);
    return RestResponse.success("디바이스가 해제되었습니다.");
  }
}
