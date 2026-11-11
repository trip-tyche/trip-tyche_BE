package com.triptyche.backend.global.telegram;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class TelegramNotifier {

  private static final String API_BASE_URL = "https://api.telegram.org/bot";

  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
  private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

  private final TelegramProperties properties;
  private final RestClient restClient;

  @Autowired
  public TelegramNotifier(TelegramProperties properties, RestClient.Builder builder) {
    this(properties, builder.requestFactory(timeoutRequestFactory()).build());
  }

  // 테스트에서 MockRestServiceServer가 붙은 RestClient를 끼워 넣기 위한 통로.
  TelegramNotifier(TelegramProperties properties, RestClient restClient) {
    this.properties = properties;
    this.restClient = restClient;
  }

  private static SimpleClientHttpRequestFactory timeoutRequestFactory() {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(CONNECT_TIMEOUT);
    factory.setReadTimeout(READ_TIMEOUT);
    return factory;
  }

  public void send(String htmlText) {
    String token = properties.botToken();
    String chatId = properties.chatId();
    // 로컬·테스트 환경은 값을 비워 두므로 조용히 건너뛴다.
    if (token == null || token.isBlank() || chatId == null || chatId.isBlank()) {
      return;
    }

    try {
      // URI 템플릿 변수로 넘기면 토큰의 ':'가 %3A로 인코딩돼 실제 API 경로와 달라진다.
      restClient.post()
          .uri(URI.create(API_BASE_URL + token + "/sendMessage"))
          .body(Map.of(
              "chat_id", chatId,
              "text", htmlText,
              "parse_mode", "HTML",
              "disable_web_page_preview", true))
          .retrieve()
          .toBodilessEntity();
    } catch (Exception e) {
      // 알림은 부가 기능이다. error로 남기면 Sentry 이벤트와 5xx 알림이 생기므로 warn으로 둔다.
      // 예외 메시지에 토큰이 박힌 URL이 들어갈 수 있어 클래스 이름만 남긴다.
      log.warn("텔레그램 알림 전송 실패: {}", e.getClass().getSimpleName());
    }
  }
}
