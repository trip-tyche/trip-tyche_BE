package com.fivefeeling.memory.service;

import com.fivefeeling.memory.entity.User;
import com.fivefeeling.memory.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;

  public User getUserById(Long userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 존재하지 않습니다."));
  }

  public User updateUserNickNameByEmail(String userEmail, String userNickName) {
    log.info("받은 이메일 : {}", userEmail);
    User user = userRepository.findByUserEmail(userEmail.trim().toLowerCase())
        .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 존재하지 않습니다."));
    user.updateUserNickName(userNickName);
    return userRepository.save(user);
  }

}
