package com.triptyche.backend.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class FirebaseConfig {

  @PostConstruct
  void initialize() {
    if (!FirebaseApp.getApps().isEmpty()) {
      return;
    }

    try {
      FirebaseApp.initializeApp(FirebaseOptions.builder()
              .setCredentials(GoogleCredentials.getApplicationDefault())
              .build());
      log.info("FirebaseApp 초기화 완료 - 푸시 발송 가능");
    } catch (IOException e) {
      log.warn("Firebase 자격증명을 찾지 못해 푸시 발송이 비활성화된다: {}", e.getMessage());
    }
  }

  public static boolean isPushAvailable() {
    return !FirebaseApp.getApps().isEmpty();
  }
}
