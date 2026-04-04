package com.triptyche.backend.global.config;

import com.triptyche.backend.global.exception.CustomAuthenticationEntryPoint;
import com.triptyche.backend.global.oauth.CustomOAuth2AuthenticationFailureHandler;
import com.triptyche.backend.global.oauth.OAuth2LoginSuccessHandler;
import com.triptyche.backend.global.oauth.service.OAuth2Service;
import com.triptyche.backend.global.util.JWTAuthenticationFilter;
import com.triptyche.backend.global.util.JwtTokenProvider;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final OAuth2Service oAuth2Service;
  private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
  private final CustomOAuth2AuthenticationFailureHandler customOAuth2AuthenticationFailureHandler;
  private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtTokenProvider jwtTokenProvider)
          throws Exception {
    http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(
                            "/swagger-ui/**",
                            "/v3/api-docs/**",
                            "/swagger-resources/**",
                            "/webjars/**",
                            "/oauth2/**",
                            "/login/**",
                            "/signin/oauth2/code/**",
                            "/signin/oauth2/authorization/**",
                            "/upload/**",
                            "/oauth2/success",
                            "/oauth2/authorization/**",
                            "/actuator/**",
                            "/v1/auth/refresh",
                            "/v1/auth/logout",
                            "/v1/auth/test-token",
                            "/ws-test.html"  // WebSocket 로컬 테스트 페이지
                    )
                    .permitAll()
                    .requestMatchers("/v1/**").authenticated()
                    .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                    .userInfoEndpoint(userInfo -> userInfo
                            .userService(oAuth2Service)
                    )
                    .successHandler(oAuth2LoginSuccessHandler)
                    .failureHandler(customOAuth2AuthenticationFailureHandler)
                    .authorizationEndpoint(authorization -> authorization
                            .baseUri("/oauth2/authorization"))  // 인증 시작 경로
                    .redirectionEndpoint(redirection -> redirection
                            .baseUri("/signin/oauth2/code/*"))  // 인증 콜백 경로
            )
            .exceptionHandling(exceptionHandling ->
                    exceptionHandling
                            .authenticationEntryPoint(customAuthenticationEntryPoint))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(new JWTAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
//    configuration.setAllowedOriginPatterns(List.of("*")); // ⭐ 여기 변경
    configuration.setAllowedOrigins(List.of(
            "https://triptyche.world",
            "https://triptychetest.shop",
            "https://www.triptychetest.shop",
            "https://local.triptyche.world:3000",
            "https://triptychetest.shop:3000",
            "https://local.triptychetest.shop:3000"
    ));
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/actuator/**", configuration);
    source.registerCorsConfiguration("/**", configuration);
    source.registerCorsConfiguration("/ws/**", configuration); // WebSocket 경로 허용
    return source;
  }

}

