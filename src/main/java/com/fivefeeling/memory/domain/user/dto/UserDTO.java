package com.fivefeeling.memory.domain.user.dto;

import com.fivefeeling.memory.domain.user.model.User;
import java.util.List;

public record UserDTO(
        String userName,
        String userEmail,
        String provider,
        List<String> roles // 권한 정보 추가
) {

  // User 엔티티로 변환하는 메서드
  public User toEntity() {
    return User.builder()
            .userName(this.userName())
            .userEmail(this.userEmail())
            .provider(this.provider())
            .build();
  }
}
