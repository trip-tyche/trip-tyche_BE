package com.triptyche.backend.global.oauth;

import com.triptyche.backend.domain.user.dto.OAuthUserInfo;
import com.triptyche.backend.global.common.ResultCode;
import com.triptyche.backend.global.exception.CustomException;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;

public enum OAuthAttributes {
  GOOGLE("google", (attribute) -> new OAuthUserInfo(
          (String) attribute.get("name"),
          (String) attribute.get("email"),
          "google"
  )),

  KAKAO("kakao", (attribute) -> {
    Map<String, Object> account = (Map) attribute.get("kakao_account");
    if (account == null) {
      throw new CustomException(ResultCode.OAUTH_SERVICE_FAILURE);
    }

    Map<String, String> profile = (Map) account.get("profile");
    if (profile == null) {
      throw new CustomException(ResultCode.OAUTH_SERVICE_FAILURE);
    }

    String email = (String) account.get("email");
    if (email == null) {
      throw new CustomException(ResultCode.OAUTH_SERVICE_FAILURE);
    }

    String nickname = profile.get("nickname");
    if (nickname == null) {
      nickname = "사용자";
    }

    return new OAuthUserInfo(
            nickname,
            email,
            "kakao"
    );
  });

  private final String registrationId;
  private final Function<Map<String, Object>, OAuthUserInfo> of;

  OAuthAttributes(String registrationId, Function<Map<String, Object>, OAuthUserInfo> of) {
    this.registrationId = registrationId;
    this.of = of;
  }

  public static OAuthUserInfo extract(String registrationId, Map<String, Object> attributes) {
    return Arrays.stream(values())
            .filter(value -> registrationId.equals(value.registrationId))
            .findFirst()
            .orElseThrow(() -> new CustomException(ResultCode.INVALID_PROVIDER))
            .of.apply(attributes);
  }

}
