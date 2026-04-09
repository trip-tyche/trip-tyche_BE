package com.triptyche.backend.domain.user.service;

import com.triptyche.backend.domain.notification.model.NotificationStatus;
import com.triptyche.backend.domain.notification.repository.NotificationRepository;
import com.triptyche.backend.domain.trip.model.TripStatus;
import com.triptyche.backend.domain.trip.repository.TripRepository;
import com.triptyche.backend.domain.user.dto.UserSearchResponse;
import com.triptyche.backend.domain.user.dto.UserSummaryResponse;
import com.triptyche.backend.domain.user.model.User;
import com.triptyche.backend.domain.user.repository.UserRepository;
import com.triptyche.backend.global.common.ResultCode;
import com.triptyche.backend.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final TripRepository tripRepository;
  private final NotificationRepository notificationRepository;

  public void updateUserNickName(User user, String userNickName) {
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
   * @param nickname
   *         검색할 사용자 닉네임
   * @return User 사용자 객체
   */

  public UserSearchResponse getUserByNickName(String nickname) {
    User user = userRepository.findByUserNickName(nickname.trim())
            .orElseThrow(() -> new CustomException(ResultCode.USER_NOT_FOUND));
    return UserSearchResponse.fromEntity(user);
  }


  public UserSummaryResponse getUserSummary(User user) {
    long tripsCount = tripRepository.countByUserAndStatus(user, TripStatus.CONFIRMED);

    long unreadNotificationCount = notificationRepository.countByUserIdAndStatus(user.getUserId(),
            NotificationStatus.UNREAD);

    return new UserSummaryResponse(
            user.getUserId(),
            user.getUserNickName(),
            tripsCount,
            unreadNotificationCount
    );
  }
}