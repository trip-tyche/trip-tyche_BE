package com.fivefeeling.memory.global.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class CookieUtil {

  private final Environment env;

  public void setCookie(HttpServletResponse response, String name, String value, int maxAge) {
    // 🔍 현재 프로파일
    String profile = env.getActiveProfiles().length > 0 ? env.getActiveProfiles()[0] : "default";

    // 조건 설정
    boolean isSecure = profile.equals("local") || profile.equals("staging");
    String sameSite = profile.equals("prod") ? "Lax" : "None";

    // Set-Cookie 헤더 직접 작성
    StringBuilder cookieHeader = new StringBuilder();
    cookieHeader.append(name).append("=").append(value)
            .append("; Max-Age=").append(maxAge)
            .append("; Path=/")
            .append("; HttpOnly")
            .append("; SameSite=").append(sameSite);

    if (isSecure) {
      cookieHeader.append("; Secure");
    }

    response.addHeader("Set-Cookie", cookieHeader.toString());
  }

  public void deleteCookie(HttpServletResponse response, String name) {
    String profile = env.getActiveProfiles().length > 0 ? env.getActiveProfiles()[0] : "default";
    boolean isSecure = profile.equals("local") || profile.equals("staging");
    String sameSite = profile.equals("prod") ? "Lax" : "None";

    StringBuilder cookieHeader = new StringBuilder();
    cookieHeader.append(name).append("=")
            .append("; Max-Age=0")
            .append("; Path=/")
            .append("; HttpOnly")
            .append("; SameSite=").append(sameSite);

    if (isSecure) {
      cookieHeader.append("; Secure");
    }
    response.addHeader("Set-Cookie", cookieHeader.toString());
  }

  /**
   * HttpServletRequest에서 지정한 이름의 쿠키 값을 반환하는 헬퍼 메서드.
   *
   * @param request
   *         HttpServletRequest
   * @param cookieName
   *         읽어올 쿠키의 이름
   * @return 해당 쿠키의 값 또는 존재하지 않으면 null 반환
   */
  public String getCookieValue(HttpServletRequest request, String cookieName) {
    if (request.getCookies() != null) {
      for (Cookie cookie : request.getCookies()) {
        if (cookieName.equals(cookie.getName())) {
          return cookie.getValue();
        }
      }
    }
    return null;
  }
}
