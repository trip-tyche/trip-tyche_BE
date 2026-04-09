package com.triptyche.backend.domain.user.dto;

import com.triptyche.backend.domain.user.model.User;
import java.util.List;

public record OAuthUserInfo(
        String userName,
        String userEmail,
        String provider,
        List<String> roles
) {

  public User toEntity() {
    return User.builder()
            .userName(this.userName())
            .userEmail(this.userEmail())
            .provider(this.provider())
            .build();
  }
}