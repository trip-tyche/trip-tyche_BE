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

// JWTAuthenticationFilter를 일부러 등록하지 않는다.
// 그 필터는 검증에 실패한 토큰이 있으면 401을 쓰고 체인을 끊으므로,
// 만료된 access 토큰을 헤더에 달고 온 갱신 요청이 컨트롤러에 닿지 못한다.
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
