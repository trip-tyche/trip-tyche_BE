package com.triptyche.backend.global.redis;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.RedisSystemException;

class StreamPollErrorHandlerTest {

  private final Logger logger = (Logger) LoggerFactory.getLogger(StreamPollErrorHandler.class);
  private ListAppender<ILoggingEvent> appender;
  private Level originalLevel;

  @BeforeEach
  void setUp() {
    appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
    originalLevel = logger.getLevel();
    logger.setLevel(Level.DEBUG);
  }

  @AfterEach
  void tearDown() {
    logger.detachAppender(appender);
    logger.setLevel(originalLevel);
  }

  @Test
  @DisplayName("컨테이너 실행 중 실패는 ERROR로 남긴다")
  void handleError_givenRunning_logsError() {
    // given
    StreamPollErrorHandler handler = new StreamPollErrorHandler(() -> true);

    // when
    handler.handleError(new RedisSystemException("Redis exception", new RuntimeException("boom")));

    // then
    assertThat(appender.list).singleElement()
        .satisfies(e -> assertThat(e.getLevel()).isEqualTo(Level.ERROR));
  }

  @Test
  @DisplayName("컨테이너 종료 중 실패는 ERROR로 남기지 않는다")
  void handleError_givenStopped_doesNotLogError() {
    // given
    StreamPollErrorHandler handler = new StreamPollErrorHandler(() -> false);

    // when
    handler.handleError(new RedisSystemException("Redis exception", new RuntimeException("Connection closed")));

    // then
    assertThat(appender.list).noneMatch(e -> e.getLevel() == Level.ERROR);
  }
}
