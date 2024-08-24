package com.fivefeeling.memory.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationHelper {

  public String getUserEmail(Authentication authentication) {
    OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
    return (String) oAuth2User.getAttributes().get("email");
  }

}
