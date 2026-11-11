package com.triptyche.backend.global.util;

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
    String profile = env.getActiveProfiles().length > 0 ? env.getActiveProfiles()[0] : "default";
    boolean isSecure = !profile.equals("local");
    String sameSite = profile.equals("prod") ? "Lax" : "None";

    StringBuilder cookieHeader = new StringBuilder();
    cookieHeader.append(name).append("=").append(value)
            .append("; Max-Age=").append(maxAge)
            .append("; Path=/")
            .append("; HttpOnly")
            .append("; SameSite=").append(sameSite);

    if (isSecure) {
      cookieHeader.append("; Secure");
      cookieHeader.append("; Domain=triptyche.cloud");
    }

    response.addHeader("Set-Cookie", cookieHeader.toString());
  }

  public void deleteCookie(HttpServletResponse response, String name) {
    String profile = env.getActiveProfiles().length > 0 ? env.getActiveProfiles()[0] : "default";
    boolean isSecure = !profile.equals("local");
    String sameSite = profile.equals("prod") ? "Lax" : "None";

    StringBuilder cookieHeader = new StringBuilder();
    cookieHeader.append(name).append("=")
            .append("; Max-Age=0")
            .append("; Path=/")
            .append("; HttpOnly")
            .append("; SameSite=").append(sameSite);

    if (isSecure) {
      cookieHeader.append("; Secure");
      cookieHeader.append("; Domain=triptyche.cloud");
    }
    response.addHeader("Set-Cookie", cookieHeader.toString());
  }

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
