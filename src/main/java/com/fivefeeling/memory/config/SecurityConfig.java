package com.fivefeeling.memory.config;

import com.fivefeeling.memory.service.OAuth2Service;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final OAuth2Service oAuth2Service;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable()) // CSRF 비활성화
        .logout(logout -> logout.disable()) // 로그아웃 비활성화
        .formLogin(formLogin -> formLogin.disable()) // 폼 로그인 비활성화
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/").permitAll() // 홈 페이지는 인증 없이 접근 가능
            .anyRequest().authenticated() // 그 외 요청은 인증 필요
        )
        .oauth2Login(oauth2 -> oauth2
            .defaultSuccessUrl("/oauth/loginInfo", true) // 로그인 성공 시 이동할 URL
            .userInfoEndpoint(userInfo -> userInfo
                .userService(oAuth2Service) // OAuth2 사용자 정보 서비스 설정
            )
        );

    return http.build();
  }
}
