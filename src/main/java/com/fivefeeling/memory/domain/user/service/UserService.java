package com.fivefeeling.memory.domain.user.service;

import com.fivefeeling.memory.domain.user.model.User;
import com.fivefeeling.memory.domain.user.repository.UserRepository;
import com.fivefeeling.memory.global.common.ResultCode;
import com.fivefeeling.memory.global.exception.CustomException;
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
        .orElseThrow(() -> new CustomException(ResultCode.USER_NOT_FOUND));
  }

  public User updateUserNickNameByEmail(String userEmail, String userNickName) {
    User user = userRepository.findByUserEmail(userEmail.trim().toLowerCase())
        .orElseThrow(() -> new CustomException(ResultCode.USER_NOT_FOUND));

    user.updateUserNickName(userNickName);
    return userRepository.save(user);
  }

}
