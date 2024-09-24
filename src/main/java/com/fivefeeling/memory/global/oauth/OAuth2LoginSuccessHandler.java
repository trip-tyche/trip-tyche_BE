package com.fivefeeling.memory.global.oauth;

import com.fivefeeling.memory.domain.user.service.UserService;
import com.fivefeeling.memory.global.util.JwtTokenProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

  private final JwtTokenProvider jwtTokenProvider;
  private final UserService userService;

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
      Authentication authentication) throws IOException, ServletException {
    // OAuth2User에서 속성 가져오기
    OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
    Map<String, Object> attributes = oAuth2User.getAttributes();

    // 속성에서 userId와 jwtToken 추출
    Long userId = attributes.get("userId") instanceof Number ? ((Number) attributes.get("userId")).longValue() : null;
    String jwtToken = attributes.get("token") instanceof String ? (String) attributes.get("token") : null;

    log.debug("userId: {}, jwtToken: {}", userId, jwtToken);
    // userId나 token이 없을 경우 에러 처리
    if (userId == null || jwtToken == null) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid user ID or token");
      return;
    }

//    // JSON 형태로 응답 작성
//    response.setContentType("application/json;charset=UTF-8");
//    response.getWriter().write("{\"token\": \"" + jwtToken + "\", \"userId\": " + userId + "}");

    response.sendRedirect("http://localhost:3000/auth/redirect?redirectedFromSocialLogin=true&token=" + jwtToken + "&userId=" + userId);
  }
}

