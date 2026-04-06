package com.triptyche.backend.global.oauth.service;

import com.triptyche.backend.domain.user.model.User;
import com.triptyche.backend.domain.user.model.UserRole;
import com.triptyche.backend.domain.user.repository.UserRepository;
import com.triptyche.backend.global.config.JwtProperties;
import com.triptyche.backend.global.util.CookieUtil;
import com.triptyche.backend.global.util.JwtTokenProvider;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuestAuthService {

    private static final String GUEST_PROVIDER = "guest";
    private static final String GUEST_NICKNAME = "게스트";

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final CookieUtil cookieUtil;

    @Transactional
    public String issueGuestToken(HttpServletResponse response) {
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String guestEmail = "guest_" + uuid + "@triptyche.com";

        User guestUser = User.builder()
                .userName(GUEST_NICKNAME)
                .userNickName(GUEST_NICKNAME + "_" + uuid)
                .userEmail(guestEmail)
                .provider(GUEST_PROVIDER)
                .role(UserRole.GUEST)
                .build();

        userRepository.save(guestUser);

        String accessToken = jwtTokenProvider.createGuestToken(guestEmail, GUEST_PROVIDER);

        cookieUtil.setCookie(response, "access_token", accessToken,
                (int) jwtProperties.guestTokenExpirySeconds());

        log.info("게스트 계정 생성: {}", guestEmail);
        return accessToken;
    }
}