package com.triptyche.backend.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppConfigProperties(
        String minSupportedVersion, String latestVersion, String updateUrl, String versionManifestUrl) {

  public AppConfigProperties {
    requireText(minSupportedVersion, "app.min-supported-version");
    requireText(latestVersion, "app.latest-version");
    requireText(updateUrl, "app.update-url");
    // version-manifest-url은 비어 있어도 된다. 비우면 latest-version 설정값을 그대로 쓴다.
  }

  private static void requireText(String value, String key) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(key + " 설정이 비어 있습니다.");
    }
  }
}
