package com.fivefeeling.memory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MemoryApplication {

  public static void main(String[] args) {
    SpringApplication.run(MemoryApplication.class, args);
  }

}
