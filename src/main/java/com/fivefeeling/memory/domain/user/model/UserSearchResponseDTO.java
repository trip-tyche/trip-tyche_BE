package com.fivefeeling.memory.domain.user.model;

public record UserSearchResponseDTO(
        Long userId,
        String userNickName
) {

  public static UserSearchResponseDTO fromEntity(User user) {
    return new UserSearchResponseDTO(user.getUserId(), user.getUserNickName());
  }

}
