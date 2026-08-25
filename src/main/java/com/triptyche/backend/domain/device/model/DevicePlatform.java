package com.triptyche.backend.domain.device.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Arrays;

public enum DevicePlatform {
  ANDROID,
  IOS;

  @JsonCreator
  public static DevicePlatform from(String value) {
    return Arrays.stream(values())
            .filter(platform -> platform.name().equalsIgnoreCase(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 platform: " + value));
  }
}
