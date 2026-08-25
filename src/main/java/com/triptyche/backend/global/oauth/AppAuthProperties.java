package com.triptyche.backend.global.oauth;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app-auth")
public record AppAuthProperties(List<String> allowedRedirects) {

  public AppAuthProperties {
    if (allowedRedirects == null || allowedRedirects.isEmpty()) {
      throw new IllegalArgumentException("app-auth.allowed-redirects 설정이 비어 있습니다.");
    }
    allowedRedirects = List.copyOf(allowedRedirects);
  }

  public int indexOf(String redirectUri) {
    return redirectUri == null ? -1 : allowedRedirects.indexOf(redirectUri);
  }

  public String redirectAt(int index) {
    return index >= 0 && index < allowedRedirects.size() ? allowedRedirects.get(index) : null;
  }
}
