package com.triptyche.backend.global.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * CORS 허용 origin 목록.
 * <p>
 * REST(SecurityConfig)와 STOMP(WebSocketConfig)가 같은 목록을 읽는다.
 * 이전에는 두 곳에 각각 하드코딩되어 있어 한 곳만 고치고 다른 곳을 잊기 쉬웠다.
 */
@ConfigurationProperties(prefix = "cors")
public record CorsProperties(List<String> allowedOrigins) {

  public CorsProperties {
    if (allowedOrigins == null || allowedOrigins.isEmpty()) {
      throw new IllegalArgumentException("cors.allowed-origins 설정이 비어 있습니다.");
    }
    allowedOrigins = List.copyOf(allowedOrigins);
  }

  public String[] toArray() {
    return allowedOrigins.toArray(new String[0]);
  }
}
