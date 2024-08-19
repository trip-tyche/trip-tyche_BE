package com.fivefeeling.memory.service;

import com.fivefeeling.memory.entity.User;
import com.fivefeeling.memory.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;

  public User getUserById(Long userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 존재하지 않습니다."));
  }

}
