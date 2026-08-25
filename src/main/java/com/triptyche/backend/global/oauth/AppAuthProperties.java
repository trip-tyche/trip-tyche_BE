package com.triptyche.backend.global.oauth;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

// 임의 스킴을 허용하면 인증 결과가 공격자 앱으로 간다.
@ConfigurationProperties(prefix = "app-auth")
public record AppAuthProperties(List<String> allowedRedirects) {

  public AppAuthProperties {
    if (allowedRedirects == null || allowedRedirects.isEmpty()) {
      throw new IllegalArgumentException("app-auth.allowed-redirects 설정이 비어 있습니다.");
    }
    allowedRedirects = List.copyOf(allowedRedirects);
  }

  public int indexOf(String redirectUri) {
    return allowedRedirects.indexOf(redirectUri);
  }

  public String redirectAt(int index) {
    return index >= 0 && index < allowedRedirects.size() ? allowedRedirects.get(index) : null;
  }
}
