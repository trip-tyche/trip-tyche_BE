package com.triptyche.backend.global.config;

import com.triptyche.backend.global.exception.CustomAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
public class AppTokenSecurityConfig {

  private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

  @Bean
  @Order(1)
  public SecurityFilterChain appTokenSecurityFilterChain(HttpSecurity http) throws Exception {
    http
            .securityMatcher("/v1/auth/token/**")
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(handling ->
                    handling.authenticationEntryPoint(customAuthenticationEntryPoint));

    return http.build();
  }
}
