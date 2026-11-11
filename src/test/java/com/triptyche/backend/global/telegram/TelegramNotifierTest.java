package com.triptyche.backend.global.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class TelegramNotifierTest {

    private static final String TOKEN = "123456:SECRET-TOKEN";
    private static final String CHAT_ID = "-1001";

    private MockRestServiceServer server;
    private RestClient restClient;
    private ListAppender<ILoggingEvent> logAppender;
    private Logger notifierLogger;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        restClient = builder.build();

        notifierLogger = (Logger) LoggerFactory.getLogger(TelegramNotifier.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        notifierLogger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        notifierLogger.detachAppender(logAppender);
    }

    private TelegramNotifier notifier(String token, String chatId) {
        return new TelegramNotifier(new TelegramProperties(token, chatId), restClient);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "  ")
    @DisplayName("봇 토큰이 비어 있으면 요청을 보내지 않는다")
    void send_givenBlankToken_doesNotCallApi(String token) {
        // given — 요청 기대를 등록하지 않았으므로, 호출이 나가면 테스트가 깨진다.

        // when
        notifier(token, CHAT_ID).send("메시지");

        // then
        server.verify();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "  ")
    @DisplayName("채팅 ID가 비어 있으면 요청을 보내지 않는다")
    void send_givenBlankChatId_doesNotCallApi(String chatId) {
        // given

        // when
        notifier(TOKEN, chatId).send("메시지");

        // then
        server.verify();
    }

    @Test
    @DisplayName("설정이 있으면 sendMessage API에 HTML 모드로 전송한다")
    void send_givenConfigured_postsHtmlMessage() {
        // given
        server.expect(requestTo("https://api.telegram.org/bot" + TOKEN + "/sendMessage"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.chat_id").value(CHAT_ID))
                .andExpect(jsonPath("$.text").value("<b>제목</b>"))
                .andExpect(jsonPath("$.parse_mode").value("HTML"))
                .andExpect(jsonPath("$.disable_web_page_preview").value(true))
                .andRespond(withSuccess("{\"ok\":true}", MediaType.APPLICATION_JSON));

        // when
        notifier(TOKEN, CHAT_ID).send("<b>제목</b>");

        // then
        server.verify();
    }

    @Test
    @DisplayName("API가 5xx를 반환해도 예외를 던지지 않고 warn만 남긴다")
    void send_givenServerError_logsWarnWithoutThrowing() {
        // given
        server.expect(requestTo("https://api.telegram.org/bot" + TOKEN + "/sendMessage"))
                .andRespond(withServerError());

        // when & then
        assertThatCode(() -> notifier(TOKEN, CHAT_ID).send("메시지")).doesNotThrowAnyException();
        assertThat(logAppender.list).hasSize(1);
        assertThat(logAppender.list.get(0).getLevel()).isEqualTo(Level.WARN);
    }

    @Test
    @DisplayName("연결 오류의 예외 메시지에 토큰이 든 URL이 있어도 로그에는 토큰이 남지 않는다")
    void send_givenIoError_doesNotLeakTokenInLog() {
        // given — RestClient는 IOException을 'I/O error on POST request for "<URL>"' 메시지로 감싼다.
        server.expect(requestTo("https://api.telegram.org/bot" + TOKEN + "/sendMessage"))
                .andRespond(withException(new IOException("connect timed out")));

        // when
        assertThatCode(() -> notifier(TOKEN, CHAT_ID).send("메시지")).doesNotThrowAnyException();

        // then
        assertThat(logAppender.list).hasSize(1);
        ILoggingEvent logged = logAppender.list.get(0);
        assertThat(logged.getLevel()).isEqualTo(Level.WARN);
        assertThat(logged.getFormattedMessage()).doesNotContain(TOKEN).doesNotContain("SECRET");
        assertThat(logged.getThrowableProxy()).isNull();
    }
}
