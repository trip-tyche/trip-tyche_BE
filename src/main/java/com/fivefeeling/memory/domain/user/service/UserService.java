package com.fivefeeling.memory.domain.user.service;

import com.fivefeeling.memory.domain.user.dto.UserSearchResponseDTO;
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

  public void updateUserNickNameByEmail(String userEmail, String userNickName) {
    User user = userRepository.findByUserEmail(userEmail.trim().toLowerCase())
            .orElseThrow(() -> new CustomException(ResultCode.USER_NOT_FOUND));

    user.updateUserNickName(userNickName);
    userRepository.save(user);
  }

  /**
   * 닉네임 중복 확인
   *
   * @param userNickName
   *         확인할 닉네임
   */
  public void validateAndCheckNickname(String userNickName) {
    if (userNickName == null || userNickName.trim().isEmpty()) {
      throw new CustomException(ResultCode.INVALID_USER_NICKNAME);
    }

    boolean isDuplicate = userRepository.existsByUserNickName(userNickName.trim());
    if (isDuplicate) {
      throw new CustomException(ResultCode.USER_NICKNAME_DUPLICATED);
    }
  }

  /**
   * 닉네임으로 사용자 검색
   *
   * @param userNickName
   *         검색할 사용자 닉네임
   * @return User 사용자 객체
   */

  public UserSearchResponseDTO getUserByNickName(String userNickName) {
    User user = userRepository.findByUserNickName(userNickName.trim())
            .orElseThrow(() -> new CustomException(ResultCode.USER_NOT_FOUND));
    return UserSearchResponseDTO.fromEntity(user);
  }
}
