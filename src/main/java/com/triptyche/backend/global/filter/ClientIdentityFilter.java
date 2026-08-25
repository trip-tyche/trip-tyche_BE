package com.triptyche.backend.global.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ClientIdentityFilter extends OncePerRequestFilter {

  public static final String CLIENT_KEY = "client";
  public static final String APP_VERSION_KEY = "appVersion";

  private static final String CLIENT_HEADER = "X-Client";
  private static final String APP_VERSION_HEADER = "X-App-Version";
  private static final String UNKNOWN = "unknown";
  private static final int MAX_LENGTH = 32;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
          throws ServletException, IOException {
    try {
      MDC.put(CLIENT_KEY, sanitize(request.getHeader(CLIENT_HEADER)));
      MDC.put(APP_VERSION_KEY, sanitize(request.getHeader(APP_VERSION_HEADER)));
      chain.doFilter(request, response);
    } finally {
      MDC.remove(CLIENT_KEY);
      MDC.remove(APP_VERSION_KEY);
    }
  }

  private String sanitize(String value) {
    if (value == null || value.isBlank()) {
      return UNKNOWN;
    }

    String cleaned = value.strip().replaceAll("[^\\w.\\-]", "");
    if (cleaned.isEmpty()) {
      return UNKNOWN;
    }
    return cleaned.length() > MAX_LENGTH ? cleaned.substring(0, MAX_LENGTH) : cleaned;
  }
}
