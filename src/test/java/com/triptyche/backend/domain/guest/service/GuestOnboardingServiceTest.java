package com.triptyche.backend.domain.guest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.triptyche.backend.domain.user.model.User;
import com.triptyche.backend.domain.user.model.UserRole;
import com.triptyche.backend.domain.user.service.UserService;
import com.triptyche.backend.global.config.JwtProperties;
import com.triptyche.backend.global.util.CookieUtil;
import com.triptyche.backend.global.util.JwtTokenProvider;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GuestOnboardingServiceTest {

    @Mock private UserService userService;
    @Mock private GuestTemplateCloneService guestTemplateCloneService;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private JwtProperties jwtProperties;
    @Mock private CookieUtil cookieUtil;

    @InjectMocks
    private GuestOnboardingService guestOnboardingService;

    private static final User GUEST_USER = User.builder()
            .userId(1L)
            .userEmail("guest_abc123@triptyche.com")
            .provider("guest")
            .role(UserRole.GUEST)
            .build();

    @Test
    @DisplayName("onboard() — 게스트 사용자 생성 후 템플릿 데이터를 복제한다")
    void onboard_clonesTemplateForGuestUser() {
        given(userService.createGuestUser()).willReturn(GUEST_USER);
        given(jwtTokenProvider.createGuestToken(anyString(), anyString())).willReturn("token");
        given(jwtProperties.guestTokenExpirySeconds()).willReturn(14400L);

        guestOnboardingService.onboard(mock(HttpServletResponse.class));

        then(guestTemplateCloneService).should().cloneForGuest(GUEST_USER);
    }

    @Test
    @DisplayName("onboard() — 생성된 액세스 토큰을 반환한다")
    void onboard_returnsAccessToken() {
        given(userService.createGuestUser()).willReturn(GUEST_USER);
        given(jwtTokenProvider.createGuestToken(anyString(), anyString())).willReturn("guest-access-token");
        given(jwtProperties.guestTokenExpirySeconds()).willReturn(14400L);

        String result = guestOnboardingService.onboard(mock(HttpServletResponse.class));

        assertThat(result).isEqualTo("guest-access-token");
    }

    @Test
    @DisplayName("onboard() — createGuestToken은 게스트 이메일과 'guest' provider로 호출된다")
    void onboard_createsGuestTokenWithCorrectEmailAndProvider() {
        given(userService.createGuestUser()).willReturn(GUEST_USER);
        given(jwtTokenProvider.createGuestToken(anyString(), anyString())).willReturn("token");
        given(jwtProperties.guestTokenExpirySeconds()).willReturn(14400L);

        guestOnboardingService.onboard(mock(HttpServletResponse.class));

        then(jwtTokenProvider).should().createGuestToken(
                eq("guest_abc123@triptyche.com"), eq("guest"));
    }
}
