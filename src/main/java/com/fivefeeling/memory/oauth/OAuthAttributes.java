package com.fivefeeling.memory.oauth;

import com.fivefeeling.memory.dto.UserDTO;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;


public enum OAuthAttributes {
  GOOGLE("google", (attribute) -> new UserDTO(
      (String) attribute.get("name"),
      (String) attribute.get("email"),
      "google"
  )),

  KAKAO("kakao", (attribute) -> {
    Map<String, Object> account = (Map) attribute.get("kakao_account");
    Map<String, String> profile = (Map) account.get("profile");

    return new UserDTO(
        profile.get("nickname"),
        (String) account.get("email"),
        "kakao"
    );
  });

  private final String registrationId;
  private final Function<Map<String, Object>, UserDTO> of;

  OAuthAttributes(String registrationId, Function<Map<String, Object>, UserDTO> of) {
    this.registrationId = registrationId;
    this.of = of;
  }

  public static UserDTO extract(String registrationId, Map<String, Object> attributes) {
    return Arrays.stream(values())
        .filter(value -> registrationId.equals(value.registrationId))
        .findFirst()
        .orElseThrow(IllegalArgumentException::new)
        .of.apply(attributes);
  }

}
