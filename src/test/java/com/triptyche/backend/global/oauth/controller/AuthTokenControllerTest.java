package com.triptyche.backend.global.oauth.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.triptyche.backend.domain.user.repository.UserRepository;
import com.triptyche.backend.global.common.ResultCode;
import com.triptyche.backend.global.config.JwtProperties;
import com.triptyche.backend.global.exception.CustomException;
import com.triptyche.backend.global.exception.GlobalExceptionHandler;
import com.triptyche.backend.global.oauth.dto.TokenIssueResponse;
import com.triptyche.backend.global.oauth.service.TokenExchangeService;
import com.triptyche.backend.global.oauth.service.TokenRefreshService;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthTokenController.class)
@Import({GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class AuthTokenControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private TokenExchangeService tokenExchangeService;

  @MockBean
  private TokenRefreshService tokenRefreshService;

  @MockBean
  private JwtProperties jwtProperties;

  // CurrentUserArgumentResolver가 내부적으로 UserRepository를 사용하므로 MockBean 등록 필요
  @MockBean
  private UserRepository userRepository;

  @Nested
  @DisplayName("POST /v1/auth/token/exchange")
  class Exchange {

    @Test
    @DisplayName("성공하면 data에 accessToken·refreshToken·expiresIn을 담아 준다")
    void exchange_givenValidCode_returnsTokenPair() throws Exception {
      // given
      given(tokenExchangeService.exchange("CODE"))
              .willReturn(new TokenIssueResponse("access.token", "refresh.token", 3600L));

      // when & then
      mockMvc.perform(post("/v1/auth/token/exchange")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content("{\"code\":\"CODE\"}"))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.data.accessToken").value("access.token"))
              .andExpect(jsonPath("$.data.refreshToken").value("refresh.token"))
              .andExpect(jsonPath("$.data.expiresIn").value(3600));
    }

    @Test
    @DisplayName("사용할 수 없는 code면 401을 준다")
    void exchange_givenUnusableCode_returnsUnauthorized() throws Exception {
      // given
      willThrow(new CustomException(ResultCode.INVALID_AUTH_CODE))
              .given(tokenExchangeService).exchange("INVALID");

      // when & then
      mockMvc.perform(post("/v1/auth/token/exchange")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content("{\"code\":\"INVALID\"}"))
              .andExpect(status().isUnauthorized())
              .andExpect(jsonPath("$.code").value(ResultCode.INVALID_AUTH_CODE.getCode()));
    }
  }

  @Nested
  @DisplayName("POST /v1/auth/token/refresh")
  class Refresh {

    @Test
    @DisplayName("성공하면 회전된 refreshToken을 함께 준다")
    void refresh_givenValidToken_returnsRotatedPair() throws Exception {
      // given
      given(tokenRefreshService.refreshToken("old.refresh"))
              .willReturn(Map.of("accessToken", "new.access", "refreshToken", "new.refresh"));
      given(jwtProperties.accessTokenExpirySeconds()).willReturn(3600L);

      // when & then
      mockMvc.perform(post("/v1/auth/token/refresh")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content("{\"refreshToken\":\"old.refresh\"}"))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.data.accessToken").value("new.access"))
              .andExpect(jsonPath("$.data.refreshToken").value("new.refresh"))
              .andExpect(jsonPath("$.data.expiresIn").value(3600));
    }

    @Test
    @DisplayName("서버에 없는 refresh 토큰이면 401을 준다")
    void refresh_givenUnknownToken_returnsUnauthorized() throws Exception {
      // given
      willThrow(new CustomException(ResultCode.REFRESH_TOKEN_EXPIRED))
              .given(tokenRefreshService).refreshToken("gone.refresh");

      // when & then
      mockMvc.perform(post("/v1/auth/token/refresh")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content("{\"refreshToken\":\"gone.refresh\"}"))
              .andExpect(status().isUnauthorized())
              .andExpect(jsonPath("$.code").value(ResultCode.REFRESH_TOKEN_EXPIRED.getCode()));
    }
  }
}
