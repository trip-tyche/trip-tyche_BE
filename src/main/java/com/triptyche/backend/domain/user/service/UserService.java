package com.triptyche.backend.domain.user.service;

import com.triptyche.backend.domain.notification.model.NotificationStatus;
import com.triptyche.backend.domain.notification.repository.NotificationRepository;
import com.triptyche.backend.domain.trip.model.TripStatus;
import com.triptyche.backend.domain.trip.repository.TripRepository;
import com.triptyche.backend.domain.user.dto.OAuthUserInfo;
import com.triptyche.backend.domain.user.dto.UserSearchResponse;
import com.triptyche.backend.domain.user.dto.UserSummaryResponse;
import com.triptyche.backend.domain.user.event.UserRegisteredEvent;
import com.triptyche.backend.domain.user.model.User;
import com.triptyche.backend.domain.user.model.UserRole;
import com.triptyche.backend.domain.user.repository.UserRepository;
import com.triptyche.backend.global.common.ResultCode;
import com.triptyche.backend.global.exception.CustomException;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final TripRepository tripRepository;
  private final NotificationRepository notificationRepository;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional
  public User getOrCreateFromOAuth(OAuthUserInfo userProfile) {
    try {
      Optional<User> existingUser = userRepository.findUserByUserEmailAndProvider(
              userProfile.userEmail(), userProfile.provider());
      if (existingUser.isPresent()) {
        User user = existingUser.get();
        user.updateUser(userProfile.userName(), userProfile.userEmail());
        return userRepository.save(user);
      }
      User newUser = userRepository.save(userProfile.toEntity());
      log.info("회원 생성: userId={}, provider={}", newUser.getUserId(), newUser.getProvider());
      eventPublisher.publishEvent(new UserRegisteredEvent(newUser.getUserId(), newUser.getProvider(), false));
      return newUser;
    } catch (DataIntegrityViolationException e) {
      log.error("이메일 중복으로 사용자 저장 실패: {}", e.getMessage());
      throw new CustomException(ResultCode.USER_SAVE_FAILURE, e);
    } catch (Exception e) {
      log.error("사용자 정보 저장 또는 업데이트 중 오류 발생: {}", e.getMessage());
      throw new CustomException(ResultCode.USER_SAVE_FAILURE, e);
    }
  }

  @Transactional
  public User createGuestUser() {
    String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    String guestEmail = "guest_" + uuid + "@triptyche.com";
    User guestUser = User.builder()
            .userName("게스트")
            .userNickName("게스트_" + uuid)
            .userEmail(guestEmail)
            .provider("guest")
            .role(UserRole.GUEST)
            .build();
    User savedGuest = userRepository.save(guestUser);
    log.info("회원 생성: userId={}, provider={}", savedGuest.getUserId(), savedGuest.getProvider());
    eventPublisher.publishEvent(new UserRegisteredEvent(savedGuest.getUserId(), savedGuest.getProvider(), true));
    return savedGuest;
  }

  @Transactional
  public void updateUserNickName(User user, String userNickName) {
    user.updateUserNickName(userNickName);
    userRepository.save(user);
  }

  public void validateAndCheckNickname(String userNickName) {
    if (userNickName == null || userNickName.trim().isEmpty()) {
      throw new CustomException(ResultCode.INVALID_USER_NICKNAME);
    }

    boolean isDuplicate = userRepository.existsByUserNickName(userNickName.trim());
    if (isDuplicate) {
      throw new CustomException(ResultCode.USER_NICKNAME_DUPLICATED);
    }
  }

  public UserSearchResponse getUserByNickName(String nickname) {
    User user = userRepository.findByUserNickName(nickname.trim())
            .orElseThrow(() -> new CustomException(ResultCode.USER_NOT_FOUND));
    return UserSearchResponse.fromEntity(user);
  }


  @Transactional(readOnly = true)
  public UserSummaryResponse getUserSummary(User user) {
    long tripsCount = tripRepository.countByUserAndStatus(user, TripStatus.CONFIRMED);

    long unreadNotificationCount = notificationRepository.countByUserIdAndStatus(user.getUserId(),
            NotificationStatus.UNREAD);

    return new UserSummaryResponse(
            user.getUserId(),
            user.getUserNickName(),
            tripsCount,
            unreadNotificationCount,
            user.getRole()
    );
  }

}