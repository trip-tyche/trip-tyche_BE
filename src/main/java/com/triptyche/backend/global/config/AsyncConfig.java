package com.triptyche.backend.global.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AsyncConfig {

  @Bean(name = "cpuBoundTaskExecutor")
  public Executor cpuBoundTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);    // CPU 코어 수에 맞게 설정 (1 vCPU)
    executor.setMaxPoolSize(2);
    executor.setQueueCapacity(100);  // 대기열 크기 조정
    executor.setThreadNamePrefix("CPU-작업-");
    executor.initialize();
    return executor;
  }

  // I/O 바운드 작업을 위한 Executor 설정 (t2.micro에 맞게 조정)
  @Bean(name = "ioBoundTaskExecutor")
  public Executor ioBoundTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(8);    // I/O 작업을 위해 스레드 수 제한
    executor.setMaxPoolSize(16);
    executor.setQueueCapacity(200); // 대기열 크기 조정
    executor.setThreadNamePrefix("IO-작업-");
    executor.initialize();
    return executor;
  }
}
