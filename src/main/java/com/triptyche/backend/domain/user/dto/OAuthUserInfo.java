package com.triptyche.backend.domain.user.dto;

import com.triptyche.backend.domain.user.model.User;

public record OAuthUserInfo(
        String userName,
        String userEmail,
        String provider
) {

  public User toEntity() {
    return User.builder()
            .userName(this.userName())
            .userEmail(this.userEmail())
            .provider(this.provider())
            .build();
  }
}