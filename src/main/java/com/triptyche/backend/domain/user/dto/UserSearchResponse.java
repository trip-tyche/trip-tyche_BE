package com.triptyche.backend.domain.user.dto;

import com.triptyche.backend.domain.user.model.User;

public record UserSearchResponse(
        Long userId,
        String nickname
) {

  public static UserSearchResponse fromEntity(User user) {
    return new UserSearchResponse(user.getUserId(), user.getUserNickName());
  }

}