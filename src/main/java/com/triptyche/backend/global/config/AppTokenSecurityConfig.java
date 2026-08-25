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
// 이 필터는 토큰이 있는데 검증에 실패하면 401을 직접 쓰고 체인을 끊는다.
// 앱은 access 토큰이 만료됐을 때 갱신을 부르므로, 그 만료 토큰이 헤더에 실려 오면
// 컨트롤러에 닿기도 전에 갱신이 막혀 사용자가 로그아웃된다.
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
