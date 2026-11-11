package com.triptyche.backend.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.triptyche.backend.global.util.JwtTokenProvider;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/**
 * MockMvc는 sendError 후의 /error 재요청(ERROR dispatch)을 재현하지 못해 실제 Tomcat으로 검증한다.
 * 이 재요청에서 인증 정보가 사라져 403이 401로 덮이던 문제를 막는다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class SecurityErrorResponseIntegrationTest {

  @LocalServerPort
  private int port;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  private final HttpClient client = HttpClient.newHttpClient();

  private HttpResponse<String> post(String path, String token) throws Exception {
    HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString("{}"));
    if (token != null) {
      builder.header("Authorization", "Bearer " + token);
    }
    return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
  }

  @Test
  @DisplayName("게스트가 쓰기 API를 호출하면 401이 아니라 403(ACCESS_DENIED)을 받는다")
  void post_givenGuestToken_returnsForbidden() throws Exception {
    // given
    String guestToken = jwtTokenProvider.createGuestToken("guest_it@triptyche.com", "guest");

    // when
    HttpResponse<String> response = post("/v1/trips", guestToken);

    // then
    assertThat(response.statusCode()).isEqualTo(403);
    assertThat(response.body()).contains("\"code\":2009");
  }

  @Test
  @DisplayName("토큰 없이 쓰기 API를 호출하면 401(UNAUTHORIZED)을 받는다")
  void post_givenNoToken_returnsUnauthorized() throws Exception {
    // when
    HttpResponse<String> response = post("/v1/trips", null);

    // then
    assertThat(response.statusCode()).isEqualTo(401);
    assertThat(response.body()).contains("\"code\":1001");
  }

  @Test
  @DisplayName("유효하지 않은 토큰은 401(INVALID_JWT 계열)을 받는다")
  void post_givenInvalidToken_returnsUnauthorized() throws Exception {
    // when
    HttpResponse<String> response = post("/v1/trips", "not-a-jwt");

    // then
    assertThat(response.statusCode()).isEqualTo(401);
  }

  @Test
  @DisplayName("일반 회원 토큰은 권한 검사를 통과한다(403·401이 아니다)")
  void post_givenUserToken_passesAuthorization() throws Exception {
    // given
    String userToken = jwtTokenProvider.createAccessToken("user_it@triptyche.com", List.of("ROLE_USER"), "kakao");

    // when
    HttpResponse<String> response = post("/v1/trips", userToken);

    // then
    assertThat(response.statusCode()).isNotIn(401, 403);
  }
}
