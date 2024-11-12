package com.fivefeeling.memory.global.oauth;

import com.fivefeeling.memory.domain.user.model.User;
import com.fivefeeling.memory.domain.user.model.UserDTO;
import com.fivefeeling.memory.domain.user.repository.UserRepository;
import com.fivefeeling.memory.global.common.ResultCode;
import com.fivefeeling.memory.global.exception.CustomException;
import com.fivefeeling.memory.global.util.JwtTokenProvider;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
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

  private final UserRepository userRepository;
  private final JwtTokenProvider jwtTokenProvider;

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
      UserDTO userProfile = OAuthAttributes.extract(registrationId, attributes);

      User user = updateOrSaveUser(userProfile);
      Long userId = user.getUserId();

      // JWT 토큰 생성
      List<String> roles = List.of("ROLE_USER"); // 기본 권한 설정
      String token = jwtTokenProvider.createToken(userProfile.userEmail(), roles, registrationId);

      Map<String, Object> customAttribute = getCustomAttribute(registrationId, userNameAttributeName, attributes, userProfile);
      customAttribute.put("token", token);
      customAttribute.put("userId", userId);

      return new DefaultOAuth2User(
          Collections.singleton(new SimpleGrantedAuthority("USER")),
          customAttribute,
          userNameAttributeName
      );
    } catch (OAuth2AuthenticationException e) {
      log.error("OAuth2 인증 중 오류 발생: {}", e.getMessage());
      throw e;
    } catch (Exception e) {
      log.error("OAuth2 서비스 처리 중 알 수 없는 오류 발생: {}", e.getMessage());
      throw new CustomException(ResultCode.INTERNAL_SERVER_ERROR, e);
    }
  }


  private Map<String, Object> getCustomAttribute(
      String registrationId,
      String userNameAttributeName,
      Map<String, Object> attributes,
      UserDTO userProfile) {
    Map<String, Object> customAttribute = new ConcurrentHashMap<>();
    customAttribute.put(userNameAttributeName, attributes.get(userNameAttributeName));
    customAttribute.put("provider", registrationId);
    customAttribute.put("name", userProfile.userName());
    customAttribute.put("email", userProfile.userEmail());
    return customAttribute;
  }

  private User updateOrSaveUser(UserDTO userProfile) {
    try {
      // 이메일과 제공자 정보를 기준으로 사용자 검색
      Optional<User> existingUser = userRepository.findUserByUserEmailAndProvider(userProfile.userEmail(), userProfile.provider());

      if (existingUser.isPresent()) {
        // 기존 사용자 업데이트
        User user = existingUser.get();
        user.updateUser(userProfile.userName(), userProfile.userEmail());
        return userRepository.save(user);
      } else {
        // 새로운 사용자 저장
        return userRepository.save(userProfile.toEntity());
      }
    } catch (DataIntegrityViolationException e) {
      log.error("이메일 중복으로 사용자 저장 실패: {}", e.getMessage());
      throw new OAuth2AuthenticationException(new OAuth2Error("email_already_registered", "이메일이 이미 등록되어 있습니다.", null), e.getMessage());
    } catch (Exception e) {
      log.error("사용자 정보 저장 또는 업데이트 중 오류 발생: {}", e.getMessage());
      throw new CustomException(ResultCode.USER_SAVE_FAILURE, e);
    }
  }
}
