package com.triptyche.backend.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.triptyche.backend.domain.notification.repository.NotificationRepository;
import com.triptyche.backend.domain.trip.repository.TripRepository;
import com.triptyche.backend.domain.user.dto.OAuthUserInfo;
import com.triptyche.backend.domain.user.model.User;
import com.triptyche.backend.domain.user.model.UserRole;
import com.triptyche.backend.domain.user.repository.UserRepository;
import com.triptyche.backend.global.common.ResultCode;
import com.triptyche.backend.global.exception.CustomException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private UserService userService;

    // -------------------------------------------------------------------------
    // validateAndCheckNickname()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("validateAndCheckNickname()")
    class ValidateAndCheckNickname {

        @Test
        @DisplayName("닉네임이 null이면 INVALID_USER_NICKNAME 예외가 발생한다")
        void validateAndCheckNickname_givenNullNickname_throwsInvalidUserNickname() {
            // given
            String nickname = null;

            // when & then
            assertThatThrownBy(() -> userService.validateAndCheckNickname(nickname))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getResultCode())
                    .isEqualTo(ResultCode.INVALID_USER_NICKNAME);
        }

        @Test
        @DisplayName("닉네임이 공백 문자열이면 INVALID_USER_NICKNAME 예외가 발생한다")
        void validateAndCheckNickname_givenBlankNickname_throwsInvalidUserNickname() {
            // given
            String nickname = "   ";

            // when & then
            assertThatThrownBy(() -> userService.validateAndCheckNickname(nickname))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getResultCode())
                    .isEqualTo(ResultCode.INVALID_USER_NICKNAME);
        }

        @Test
        @DisplayName("이미 사용 중인 닉네임이면 USER_NICKNAME_DUPLICATED 예외가 발생한다")
        void validateAndCheckNickname_givenDuplicateNickname_throwsUserNicknameDuplicated() {
            // given
            String nickname = "중복닉네임";
            given(userRepository.existsByUserNickName(nickname)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> userService.validateAndCheckNickname(nickname))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getResultCode())
                    .isEqualTo(ResultCode.USER_NICKNAME_DUPLICATED);
        }

        @Test
        @DisplayName("유효하고 중복되지 않은 닉네임이면 예외 없이 정상 통과한다")
        void validateAndCheckNickname_givenValidNickname_doesNotThrow() {
            // given
            String nickname = "사용가능한닉네임";
            given(userRepository.existsByUserNickName(nickname)).willReturn(false);

            // when & then — 예외 없이 종료되어야 한다
            userService.validateAndCheckNickname(nickname);

            verify(userRepository).existsByUserNickName(nickname);
        }

        @Test
        @DisplayName("닉네임 앞뒤 공백은 trim 처리 후 중복 검사에 사용된다")
        void validateAndCheckNickname_givenNicknameWithWhitespace_trimmedBeforeDuplicateCheck() {
            // given
            String nickname = "  닉네임  ";
            given(userRepository.existsByUserNickName("닉네임")).willReturn(false);

            // when
            userService.validateAndCheckNickname(nickname);

            // then
            verify(userRepository).existsByUserNickName("닉네임");
        }
    }

    // -------------------------------------------------------------------------
    // getOrCreateFromOAuth()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("getOrCreateFromOAuth()")
    class GetOrCreateFromOAuth {

        private final OAuthUserInfo newUserInfo =
                new OAuthUserInfo("신규유저", "new@example.com", "google");

        private final OAuthUserInfo existingUserInfo =
                new OAuthUserInfo("업데이트된이름", "existing@example.com", "google");

        @Test
        @DisplayName("이미 가입된 사용자가 OAuth 로그인하면 updateUser()가 호출된 뒤 save된 User를 반환한다")
        void getOrCreateFromOAuth_givenExistingUser_updatesAndReturnsUser() {
            // given
            User existingUser = User.builder()
                    .userId(1L)
                    .userName("기존이름")
                    .userEmail("existing@example.com")
                    .provider("google")
                    .build();

            given(userRepository.findUserByUserEmailAndProvider(
                    existingUserInfo.userEmail(), existingUserInfo.provider()))
                    .willReturn(Optional.of(existingUser));
            given(userRepository.save(existingUser)).willReturn(existingUser);

            // when
            User result = userService.getOrCreateFromOAuth(existingUserInfo);

            // then
            assertThat(result.getUserName()).isEqualTo("업데이트된이름");
            verify(userRepository).save(existingUser);
        }

        @Test
        @DisplayName("신규 사용자가 OAuth 로그인하면 toEntity()로 생성된 User가 save되어 반환된다")
        void getOrCreateFromOAuth_givenNewUser_savesAndReturnsNewUser() {
            // given
            User savedUser = User.builder()
                    .userId(2L)
                    .userName(newUserInfo.userName())
                    .userEmail(newUserInfo.userEmail())
                    .provider(newUserInfo.provider())
                    .build();

            given(userRepository.findUserByUserEmailAndProvider(
                    newUserInfo.userEmail(), newUserInfo.provider()))
                    .willReturn(Optional.empty());
            given(userRepository.save(any(User.class))).willReturn(savedUser);

            // when
            User result = userService.getOrCreateFromOAuth(newUserInfo);

            // then
            assertThat(result.getUserEmail()).isEqualTo("new@example.com");
            assertThat(result.getProvider()).isEqualTo("google");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("save 중 DataIntegrityViolationException 발생 시 USER_SAVE_FAILURE 예외로 변환된다")
        void getOrCreateFromOAuth_givenDataIntegrityViolation_throwsUserSaveFailure() {
            // given
            given(userRepository.findUserByUserEmailAndProvider(
                    newUserInfo.userEmail(), newUserInfo.provider()))
                    .willReturn(Optional.empty());
            given(userRepository.save(any(User.class)))
                    .willThrow(new DataIntegrityViolationException("unique constraint violation"));

            // when & then
            assertThatThrownBy(() -> userService.getOrCreateFromOAuth(newUserInfo))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getResultCode())
                    .isEqualTo(ResultCode.USER_SAVE_FAILURE);
        }

        @Test
        @DisplayName("save 중 일반 Exception 발생 시 USER_SAVE_FAILURE 예외로 변환된다")
        void getOrCreateFromOAuth_givenUnexpectedException_throwsUserSaveFailure() {
            // given
            given(userRepository.findUserByUserEmailAndProvider(
                    newUserInfo.userEmail(), newUserInfo.provider()))
                    .willReturn(Optional.empty());
            given(userRepository.save(any(User.class)))
                    .willThrow(new RuntimeException("unexpected error"));

            // when & then
            assertThatThrownBy(() -> userService.getOrCreateFromOAuth(newUserInfo))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getResultCode())
                    .isEqualTo(ResultCode.USER_SAVE_FAILURE);
        }
    }

    // -------------------------------------------------------------------------
    // createGuestUser()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("createGuestUser()")
    class CreateGuestUser {

        @Test
        @DisplayName("생성된 게스트 사용자의 role은 GUEST이다")
        void createGuestUser_givenCall_returnsUserWithGuestRole() {
            // given
            given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            User result = userService.createGuestUser();

            // then
            assertThat(result.getRole()).isEqualTo(UserRole.GUEST);
        }

        @Test
        @DisplayName("생성된 게스트 사용자의 provider는 'guest'이다")
        void createGuestUser_givenCall_returnsUserWithGuestProvider() {
            // given
            given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            User result = userService.createGuestUser();

            // then
            assertThat(result.getProvider()).isEqualTo("guest");
        }

        @Test
        @DisplayName("생성된 게스트 사용자의 이메일은 'guest_' 접두사와 '@triptyche.com' 접미사를 갖는다")
        void createGuestUser_givenCall_returnsUserWithCorrectEmailFormat() {
            // given
            given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            User result = userService.createGuestUser();

            // then
            assertThat(result.getUserEmail())
                    .startsWith("guest_")
                    .endsWith("@triptyche.com");
        }

        @Test
        @DisplayName("게스트 사용자 생성 시 userRepository.save()가 정확히 한 번 호출된다")
        void createGuestUser_givenCall_savesUserExactlyOnce() {
            // given
            given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            userService.createGuestUser();

            // then
            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            User savedUser = captor.getValue();
            assertThat(savedUser.getRole()).isEqualTo(UserRole.GUEST);
            assertThat(savedUser.getProvider()).isEqualTo("guest");
        }
    }
}