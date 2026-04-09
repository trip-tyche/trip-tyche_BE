package com.triptyche.backend.global.oauth.service;

import com.triptyche.backend.domain.user.dto.OAuthUserInfo;
import com.triptyche.backend.domain.user.model.User;
import com.triptyche.backend.domain.user.service.UserService;
import com.triptyche.backend.global.common.ResultCode;
import com.triptyche.backend.global.exception.CustomException;
import com.triptyche.backend.global.oauth.OAuthAttributes;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuth2Service implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

  private final UserService userService;

  @Override
  public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
    try {
      OAuth2UserService<OAuth2UserRequest, OAuth2User> oAuth2UserService = new DefaultOAuth2UserService();
      OAuth2User oAuth2User = oAuth2UserService.loadUser(userRequest);

      String registrationId = userRequest.getClientRegistration().getRegistrationId();
      String userNameAttributeName = userRequest.getClientRegistration()
              .getProviderDetails()
              .getUserInfoEndpoint()
              .getUserNameAttributeName();

      Map<String, Object> attributes = oAuth2User.getAttributes();
      OAuthUserInfo userProfile = OAuthAttributes.extract(registrationId, attributes);

      User user = userService.getOrCreateFromOAuth(userProfile);
      Long userId = user.getUserId();

      Map<String, Object> customAttribute = getCustomAttribute(registrationId, userNameAttributeName, attributes,
              userProfile);
      customAttribute.put("userId", userId);

      return new DefaultOAuth2User(
              Collections.singleton(new SimpleGrantedAuthority("USER")),
              customAttribute,
              userNameAttributeName
      );
    } catch (OAuth2AuthenticationException e) {
      log.error("OAuth2 인증 중 오류 발생: {}", e.getMessage());
      throw e;
    } catch (CustomException e) {
      log.error("사용자 저장 실패: {}", e.getMessage());
      throw new OAuth2AuthenticationException(new OAuth2Error("user_save_failure", e.getMessage(), null), e.getMessage());
    } catch (Exception e) {
      log.error("OAuth2 서비스 처리 중 알 수 없는 오류 발생: {}", e.getMessage());
      throw new CustomException(ResultCode.INTERNAL_SERVER_ERROR, e);
    }
  }

  private Map<String, Object> getCustomAttribute(
          String registrationId,
          String userNameAttributeName,
          Map<String, Object> attributes,
          OAuthUserInfo userProfile) {
    Map<String, Object> customAttribute = new HashMap<>();
    customAttribute.put(userNameAttributeName, attributes.get(userNameAttributeName));
    customAttribute.put("provider", registrationId);
    customAttribute.put("name", userProfile.userName());
    customAttribute.put("email", userProfile.userEmail());
    return customAttribute;
  }
}
