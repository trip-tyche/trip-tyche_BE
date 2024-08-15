package com.fivefeeling.memory.dto;

import com.fivefeeling.memory.entity.User;

public record UserDTO(
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


