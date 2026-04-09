package com.triptyche.backend.global.oauth.service;

import com.triptyche.backend.domain.user.model.User;
import com.triptyche.backend.domain.user.service.UserService;
import com.triptyche.backend.global.config.JwtProperties;
import com.triptyche.backend.global.util.CookieUtil;
import com.triptyche.backend.global.util.JwtTokenProvider;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuestAuthService {

    private static final String GUEST_PROVIDER = "guest";

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final CookieUtil cookieUtil;

    public String issueGuestToken(HttpServletResponse response) {
        User guestUser = userService.createGuestUser();
        String guestEmail = guestUser.getUserEmail();

        String accessToken = jwtTokenProvider.createGuestToken(guestEmail, GUEST_PROVIDER);

        cookieUtil.setCookie(response, "access_token", accessToken,
                (int) jwtProperties.guestTokenExpirySeconds());

        log.info("게스트 계정 생성: {}", guestEmail);
        return accessToken;
    }
}