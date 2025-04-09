package com.fivefeeling.memory.global.util;

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
}
