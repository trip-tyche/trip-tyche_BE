package com.triptyche.backend.global.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

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
