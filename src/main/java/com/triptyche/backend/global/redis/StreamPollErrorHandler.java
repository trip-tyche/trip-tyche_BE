package com.triptyche.backend.global.redis;

import java.util.function.BooleanSupplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ErrorHandler;

/**
 * 컨테이너 stop()은 진행 중인 XREADGROUP(pollTimeout)을 기다리지 않는다. 직후 커넥션 팩토리가 닫히면
 * 그 읽기가 "Connection closed"로 실패하는데, 기본 핸들러는 이를 ERROR로 남겨 배포마다 Sentry 이벤트가 생긴다.
 * 종료 중의 실패만 debug로 낮추고, 실행 중의 실패는 그대로 ERROR로 남긴다.
 */
@Slf4j
public class StreamPollErrorHandler implements ErrorHandler {

  private final BooleanSupplier containerRunning;

  public StreamPollErrorHandler(BooleanSupplier containerRunning) {
    this.containerRunning = containerRunning;
  }

  @Override
  public void handleError(Throwable t) {
    if (containerRunning.getAsBoolean()) {
      log.error("Redis Stream 폴링 실패", t);
      return;
    }
    log.debug("종료 중 Redis Stream 폴링 중단: {}", t.getMessage());
  }
}
