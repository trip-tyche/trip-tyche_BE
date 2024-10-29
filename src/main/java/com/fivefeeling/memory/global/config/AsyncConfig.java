package com.fivefeeling.memory.global.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AsyncConfig {

  @Bean(name = "fileProcessingTaskExecutor")
  public Executor fileProcessingTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(20);  // 기본 유지 스레드 수
    executor.setMaxPoolSize(25);   // 최대 스레드 수
    executor.setQueueCapacity(50); // 대기열 크기
    executor.setThreadNamePrefix("파일업로드-");
    executor.initialize();
    return executor;
  }
}
