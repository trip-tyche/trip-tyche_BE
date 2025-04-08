package com.fivefeeling.memory.global.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class CookieUtil {

  private final Environment env;

  public void setCookie(HttpServletResponse response, String name, String value, int maxAge) {
    Cookie cookie = new Cookie(name, value);
    cookie.setHttpOnly(true);
    cookie.setPath("/");
    cookie.setMaxAge(maxAge);

    // 🔍 현재 프로파일 가져오기
    String profile = env.getActiveProfiles().length > 0 ? env.getActiveProfiles()[0] : "default";

    // 🔧 조건별로 Secure, SameSite 설정
    boolean isSecure = profile.equals("local") || profile.equals("staging");
    String sameSite = profile.equals("prod") ? "Lax" : "None";

    if (isSecure) {
      cookie.setSecure(true); // HTTPS 환경에서만 전송
    }

    response.addCookie(cookie); // 실제 쿠키 등록

    // Spring의 Cookie 객체는 SameSite 설정이 안 되므로 Header 직접 추가
    String cookieHeader = String.format("%s=%s; Max-Age=%d; Path=/; HttpOnly; SameSite=%s%s",
            name, value, maxAge, sameSite, isSecure ? "; Secure" : "");
    response.addHeader("Set-Cookie", cookieHeader);
  }
}
