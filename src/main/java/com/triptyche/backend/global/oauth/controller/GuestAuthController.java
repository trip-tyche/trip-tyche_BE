package com.triptyche.backend.global.oauth.controller;

import com.triptyche.backend.global.common.RestResponse;
import com.triptyche.backend.global.oauth.service.GuestAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "0. 로그인&인증관련 API")
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class GuestAuthController {

    private final GuestAuthService guestAuthService;

    @Operation(summary = "게스트 토큰 발급 (포트폴리오 체험용)")
    @PostMapping("/guest")
    public RestResponse<String> issueGuestToken(HttpServletResponse response) {
        guestAuthService.issueGuestToken(response);
        return RestResponse.success("게스트 로그인 성공. 4시간 동안 모든 기능을 체험하실 수 있습니다.");
    }
}