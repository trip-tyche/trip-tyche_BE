package com.triptyche.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TriptycheApplication {

  public static void main(String[] args) {
    SpringApplication.run(TriptycheApplication.class, args);
  }

}
