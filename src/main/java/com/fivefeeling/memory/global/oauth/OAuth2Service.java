package com.fivefeeling.memory.global.oauth;

import com.fivefeeling.memory.domain.user.model.User;
import com.fivefeeling.memory.domain.user.model.UserDTO;
import com.fivefeeling.memory.domain.user.repository.UserRepository;
import com.fivefeeling.memory.global.util.JwtTokenProvider;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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

    updateOrSaveUser(userProfile);

    Map<String, Object> customAttribute = getCustomAttribute(registrationId, userNameAttributeName, attributes, userProfile);

    // JWT 토큰 생성 후 추가적으로 설정
    List<String> roles = List.of("ROLE_USER"); // 기본 권한 설정 (예: USER)
    String token = jwtTokenProvider.createToken(userProfile.userEmail(), roles);
    customAttribute.put("token", token);  // 클라이언트에 JWT 토큰 전달

    return new DefaultOAuth2User(
        Collections.singleton(new SimpleGrantedAuthority("USER")),
        customAttribute,
        userNameAttributeName
    );
  }

  private Map<String, Object> getCustomAttribute(String registrationId,
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
    User user = userRepository.findUserByUserEmailAndProvider(userProfile.userEmail(), userProfile.provider())
        .map(value -> value.updateUser(userProfile.userName(), userProfile.userEmail()))
        .orElse(userProfile.toEntity());

    return userRepository.save(user);
  }

}
