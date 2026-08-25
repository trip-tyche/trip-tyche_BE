package com.triptyche.backend.global.oauth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.triptyche.backend.domain.user.repository.UserRepository;
import com.triptyche.backend.global.config.JwtProperties;
import com.triptyche.backend.global.exception.GlobalExceptionHandler;
import com.triptyche.backend.global.oauth.service.LogoutService;
import com.triptyche.backend.global.oauth.service.TokenRefreshService;
import com.triptyche.backend.global.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import({GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

  private static final String COOKIE_REFRESH = "cookie.refresh.token";
  private static final String BEARER_ACCESS = "bearer.access.token";

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private TokenRefreshService tokenRefreshService;

  @MockBean
  private CookieUtil cookieUtil;

  @MockBean
  private LogoutService logoutService;

  @MockBean
  private JwtProperties jwtProperties;

  // CurrentUserArgumentResolver가 내부적으로 UserRepository를 사용하므로 MockBean 등록 필요
  @MockBean
  private UserRepository userRepository;

  @Nested
  @DisplayName("POST /v1/auth/logout")
  class Logout {

    @Test
    @DisplayName("쿠키가 있으면 쿠키의 refresh 토큰으로 세션을 무효화한다")
    void logout_givenRefreshCookie_invalidatesCookieSession() throws Exception {
      // given
      given(cookieUtil.getCookieValue(any(HttpServletRequest.class), eq("refresh_token"))).willReturn(COOKIE_REFRESH);

      // when & then
      mockMvc.perform(post("/v1/auth/logout")).andExpect(status().isOk());
      verify(logoutService).logout(COOKIE_REFRESH);
    }

    @Test
    @DisplayName("쿠키가 없고 Bearer만 있으면 access 토큰으로 세션을 무효화한다")
    void logout_givenOnlyBearer_invalidatesBearerSession() throws Exception {
      // given
      given(cookieUtil.getCookieValue(any(HttpServletRequest.class), eq("refresh_token"))).willReturn(null);

      // when & then
      mockMvc.perform(post("/v1/auth/logout").header("Authorization", "Bearer " + BEARER_ACCESS))
              .andExpect(status().isOk());
      verify(logoutService).logout(BEARER_ACCESS);
    }

    @Test
    @DisplayName("쿠키가 있으면 Bearer가 함께 와도 쿠키를 우선한다")
    void logout_givenBothCookieAndBearer_prefersCookie() throws Exception {
      // given
      given(cookieUtil.getCookieValue(any(HttpServletRequest.class), eq("refresh_token"))).willReturn(COOKIE_REFRESH);

      // when & then
      mockMvc.perform(post("/v1/auth/logout").header("Authorization", "Bearer " + BEARER_ACCESS))
              .andExpect(status().isOk());
      verify(logoutService).logout(COOKIE_REFRESH);
      verify(logoutService, never()).logout(BEARER_ACCESS);
    }

    @Test
    @DisplayName("쿠키도 Bearer도 없으면 무효화를 시도하지 않고 쿠키만 지운다")
    void logout_givenNeither_skipsInvalidation() throws Exception {
      // given
      given(cookieUtil.getCookieValue(any(HttpServletRequest.class), eq("refresh_token"))).willReturn(null);

      // when & then
      mockMvc.perform(post("/v1/auth/logout")).andExpect(status().isOk());
      verify(logoutService, never()).logout(anyString());
      verify(cookieUtil).deleteCookie(any(), eq("access_token"));
      verify(cookieUtil).deleteCookie(any(), eq("refresh_token"));
    }
  }
}
