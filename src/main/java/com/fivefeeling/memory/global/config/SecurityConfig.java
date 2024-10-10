package com.fivefeeling.memory.global.config;

import com.fivefeeling.memory.global.oauth.OAuth2LoginSuccessHandler;
import com.fivefeeling.memory.global.oauth.OAuth2Service;
import com.fivefeeling.memory.global.util.JWTAuthenticationFilter;
import com.fivefeeling.memory.global.util.JwtTokenProvider;
import io.jsonwebtoken.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final OAuth2Service oAuth2Service;
  private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtTokenProvider jwtTokenProvider) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(
                "/swagger-ui/**",
                "/v3/api-docs/**",
                "/swagger-resources/**",
                "/webjars/**",
                "/",
                "/oauth2/**",
                "/api/**",
                "/api/user/nickname",
                "/nickname",
                "/login/oauth2/code/**",
                "/upload/**",
                "/oauth2/success",
                "/oauth2/authorization/**")
            .permitAll()
            .anyRequest().authenticated()
        )
        .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(oAuth2Service)
                )
                .successHandler(oAuth2LoginSuccessHandler)
//            .defaultSuccessUrl("/swagger-ui/index.html#/")
        )
        .logout(logout -> logout.logoutSuccessUrl("/"))
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .addFilterBefore(new JWTAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(new IPLoggingFilter(), JWTAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(
        List.of("http://localhost:3000", "http://ec2-43-203-13-252.ap-northeast-2.compute.amazonaws.com", "http://trip-tyche-fe.s3-website.ap-northeast-2.amazonaws.com"));
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  // IPLoggingFilter 클래스 추가
  public class IPLoggingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(IPLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException, java.io.IOException {
      String clientIp = request.getRemoteAddr();
      logger.info("Request from IP: {}", clientIp);
      filterChain.doFilter(request, response);
    }
  }
}

