package com.triptyche.backend.domain.user.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.triptyche.backend.domain.user.model.UserRole;
import com.triptyche.backend.domain.user.repository.UserRepository;
import com.triptyche.backend.global.telegram.TelegramNotifier;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserRegisteredTelegramListenerTest {

    // 서울 2026-11-11 14:30
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-11T05:30:00Z"), ZoneOffset.UTC);

    @Mock
    private TelegramNotifier telegramNotifier;

    @Mock
    private UserRepository userRepository;

    private UserRegisteredTelegramListener listener() {
        return new UserRegisteredTelegramListener(telegramNotifier, userRepository, CLOCK);
    }

    private String sentMessage() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(telegramNotifier).send(captor.capture());
        return captor.getValue();
    }

    @Nested
    @DisplayName("신규 가입 알림")
    class Signup {

        @Test
        @DisplayName("카카오 가입이면 카카오 라벨과 누적 회원 수가 담긴 메시지를 보낸다")
        void onUserRegistered_givenKakaoSignup_sendsSignupMessage() {
            // given
            given(userRepository.countByRole(UserRole.USER)).willReturn(45L);

            // when
            listener().onUserRegistered(new UserRegisteredEvent(123L, "kakao", false));

            // then
            assertThat(sentMessage()).isEqualTo("""
                    🎉 <b>신규 가입</b>
                    🔑 카카오 · #123
                    👥 누적 회원 45명""");
        }

        @Test
        @DisplayName("구글 가입이면 구글 라벨로 표시한다")
        void onUserRegistered_givenGoogleSignup_sendsGoogleLabel() {
            // given
            given(userRepository.countByRole(UserRole.USER)).willReturn(46L);

            // when
            listener().onUserRegistered(new UserRegisteredEvent(124L, "google", false));

            // then
            assertThat(sentMessage()).isEqualTo("""
                    🎉 <b>신규 가입</b>
                    🔑 구글 · #124
                    👥 누적 회원 46명""");
        }

        @Test
        @DisplayName("알 수 없는 provider는 원문 그대로 표시한다")
        void onUserRegistered_givenUnknownProvider_sendsRawProvider() {
            // given
            given(userRepository.countByRole(UserRole.USER)).willReturn(47L);

            // when
            listener().onUserRegistered(new UserRegisteredEvent(125L, "apple", false));

            // then
            assertThat(sentMessage()).contains("🔑 apple · #125");
        }

        @Test
        @DisplayName("누적 회원 수 집계가 실패해도 앞의 두 줄은 보낸다")
        void onUserRegistered_givenCountFailure_sendsFirstTwoLines() {
            // given
            given(userRepository.countByRole(UserRole.USER)).willThrow(new IllegalStateException("db down"));

            // when
            listener().onUserRegistered(new UserRegisteredEvent(123L, "kakao", false));

            // then
            assertThat(sentMessage()).isEqualTo("""
                    🎉 <b>신규 가입</b>
                    🔑 카카오 · #123""");
        }
    }

    @Nested
    @DisplayName("게스트 입장 알림")
    class Guest {

        @Test
        @DisplayName("게스트 입장이면 오늘 게스트 수가 담긴 메시지를 보낸다")
        void onUserRegistered_givenGuest_sendsGuestMessage() {
            // given
            given(userRepository.countByRoleAndCreatedAtGreaterThanEqual(any(), any())).willReturn(7L);

            // when
            listener().onUserRegistered(new UserRegisteredEvent(124L, "guest", true));

            // then
            assertThat(sentMessage()).isEqualTo("""
                    👀 <b>게스트 입장</b>
                    🆔 #124
                    📅 오늘 게스트 7명""");
        }

        @Test
        @DisplayName("UTC로는 전날이어도 서울 기준 오늘 00:00부터 센다")
        void onUserRegistered_givenUtcPreviousDay_countsFromSeoulMidnight() {
            // given — UTC 2026-11-10 16:00 = 서울 2026-11-11 01:00
            Clock clock = Clock.fixed(Instant.parse("2026-11-10T16:00:00Z"), ZoneOffset.UTC);
            UserRegisteredTelegramListener listener =
                    new UserRegisteredTelegramListener(telegramNotifier, userRepository, clock);
            given(userRepository.countByRoleAndCreatedAtGreaterThanEqual(
                    UserRole.GUEST, LocalDateTime.of(2026, 11, 11, 0, 0))).willReturn(3L);

            // when
            listener.onUserRegistered(new UserRegisteredEvent(124L, "guest", true));

            // then
            assertThat(sentMessage()).endsWith("📅 오늘 게스트 3명");
        }

        @Test
        @DisplayName("오늘 게스트 수 집계가 실패해도 앞의 두 줄은 보낸다")
        void onUserRegistered_givenCountFailure_sendsFirstTwoLines() {
            // given
            given(userRepository.countByRoleAndCreatedAtGreaterThanEqual(any(), any()))
                    .willThrow(new IllegalStateException("db down"));

            // when
            listener().onUserRegistered(new UserRegisteredEvent(124L, "guest", true));

            // then
            assertThat(sentMessage()).isEqualTo("""
                    👀 <b>게스트 입장</b>
                    🆔 #124""");
        }
    }
}
