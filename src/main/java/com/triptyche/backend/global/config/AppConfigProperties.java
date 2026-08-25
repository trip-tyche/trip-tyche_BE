package com.triptyche.backend.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppConfigProperties(String minSupportedVersion, String latestVersion, String updateUrl) {

  public AppConfigProperties {
    requireText(minSupportedVersion, "app.min-supported-version");
    requireText(latestVersion, "app.latest-version");
    requireText(updateUrl, "app.update-url");
  }

  private static void requireText(String value, String key) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(key + " 설정이 비어 있습니다.");
    }
  }
}
