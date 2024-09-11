package com.fivefeeling.memory.global.oauth;

import com.fivefeeling.memory.domain.user.model.User;
import com.fivefeeling.memory.domain.user.model.UserDTO;
import com.fivefeeling.memory.domain.user.repository.UserRepository;
import com.fivefeeling.memory.global.util.JwtTokenProvider;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OAuth2Service implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

  private final UserRepository userRepository;
  private final JwtTokenProvider jwtTokenProvider;

  @Override
  public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
    OAuth2UserService oAuth2UserService = new DefaultOAuth2UserService();
    OAuth2User oAuth2User = oAuth2UserService.loadUser(userRequest);

    String registrationId = userRequest.getClientRegistration().getRegistrationId();
    String userNameAttributeName = userRequest.getClientRegistration()
        .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

    Map<String, Object> attributes = oAuth2User.getAttributes();
    UserDTO userProfile = OAuthAttributes.extract(registrationId, attributes);

    User user = updateOrSaveUser(userProfile);
    Long userId = user.getUserId();

    // JWT 토큰 생성 후 추가적으로 설정
    List<String> roles = List.of("ROLE_USER"); // 기본 권한 설정 (예: USER)
    String token = jwtTokenProvider.createToken(userProfile.userEmail(), roles);

    Map<String, Object> customAttribute = getCustomAttribute(registrationId, userNameAttributeName, attributes, userProfile);
    customAttribute.put("token", token);
    customAttribute.put("userId", userId);

    return new DefaultOAuth2User(
        Collections.singleton(new SimpleGrantedAuthority("USER")),
        customAttribute,
        userNameAttributeName
    );
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
    // 이메일과 제공자 정보를 기준으로 사용자 검색
    Optional<User> existingUser = userRepository.findUserByUserEmailAndProvider(userProfile.userEmail(), userProfile.provider());

    // 이미 존재하는 사용자인 경우 업데이트, 그렇지 않으면 새로운 사용자 저장
    if (existingUser.isPresent()) {
      // 기존 사용자 업데이트
      User user = existingUser.get();
      user.updateUser(userProfile.userName(), userProfile.userEmail());
      return userRepository.save(user);
    } else {
      // 새로운 사용자 저장
      return userRepository.save(userProfile.toEntity());
    }
  }
}
