package com.triptyche.backend.domain.user.event;

import com.triptyche.backend.domain.user.model.UserRole;
import com.triptyche.backend.domain.user.repository.UserRepository;
import com.triptyche.backend.global.telegram.TelegramNotifier;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class UserRegisteredTelegramListener {

  private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

  private final TelegramNotifier telegramNotifier;
  private final UserRepository userRepository;
  private final Clock clock;

  @Autowired
  public UserRegisteredTelegramListener(TelegramNotifier telegramNotifier, UserRepository userRepository) {
    this(telegramNotifier, userRepository, Clock.systemUTC());
  }

  // 테스트에서 고정 시계를 끼워 넣기 위한 통로.
  UserRegisteredTelegramListener(TelegramNotifier telegramNotifier, UserRepository userRepository, Clock clock) {
    this.telegramNotifier = telegramNotifier;
    this.userRepository = userRepository;
    this.clock = clock;
  }

  @Async("pushTaskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onUserRegistered(UserRegisteredEvent event) {
    String message = event.guest() ? guestMessage(event) : signupMessage(event);
    telegramNotifier.send(message);
  }

  private String signupMessage(UserRegisteredEvent event) {
    String message = "🎉 <b>신규 가입</b>\n🔑 %s · #%d".formatted(providerLabel(event.provider()), event.userId());
    try {
      return message + "\n👥 누적 회원 %d명".formatted(userRepository.countByRole(UserRole.USER));
    } catch (Exception e) {
      log.warn("가입 알림의 누적 회원 수 집계 실패: {}", e.toString());
      return message;
    }
  }

  private String guestMessage(UserRegisteredEvent event) {
    String message = "👀 <b>게스트 입장</b>\n🆔 #%d".formatted(event.userId());
    try {
      // createdAt은 JVM 기본 시간대(운영은 TZ=Asia/Seoul)의 LocalDateTime이라 서울 자정과 그대로 비교한다.
      LocalDateTime startOfToday = LocalDate.now(clock.withZone(SEOUL)).atStartOfDay();
      long todayGuests = userRepository.countByRoleAndCreatedAtGreaterThanEqual(UserRole.GUEST, startOfToday);
      return message + "\n📅 오늘 게스트 %d명".formatted(todayGuests);
    } catch (Exception e) {
      log.warn("게스트 알림의 오늘 게스트 수 집계 실패: {}", e.toString());
      return message;
    }
  }

  private static String providerLabel(String provider) {
    return switch (provider) {
      case "kakao" -> "카카오";
      case "google" -> "구글";
      default -> provider;
    };
  }
}
