package com.fivefeeling.memory.domain.user.dto;

import com.fivefeeling.memory.domain.user.model.User;

public record UserSearchResponseDTO(
        Long userId,
        String nickname
) {

  public static UserSearchResponseDTO fromEntity(User user) {
    return new UserSearchResponseDTO(user.getUserId(), user.getUserNickName());
  }

}
