package com.triptyche.backend.global.filter;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ClientIdentityFilterTest {

  private final ClientIdentityFilter filter = new ClientIdentityFilter();

  private Map<String, String> captureDuringChain(MockHttpServletRequest request) throws Exception {
    Map<String, String> captured = new HashMap<>();
    FilterChain chain = (req, res) -> {
      captured.put(ClientIdentityFilter.CLIENT_KEY, MDC.get(ClientIdentityFilter.CLIENT_KEY));
      captured.put(ClientIdentityFilter.APP_VERSION_KEY, MDC.get(ClientIdentityFilter.APP_VERSION_KEY));
    };
    filter.doFilter(request, new MockHttpServletResponse(), chain);
    return captured;
  }

  @Nested
  @DisplayName("헤더가 있을 때")
  class WithHeaders {

    @Test
    @DisplayName("헤더 값이 MDC에 담긴다")
    void doFilter_givenHeaders_putsThemInMdc() throws Exception {
      // given
      MockHttpServletRequest request = new MockHttpServletRequest();
      request.addHeader("X-Client", "app-android");
      request.addHeader("X-App-Version", "1.0.0");

      // when
      Map<String, String> captured = captureDuringChain(request);

      // then
      assertThat(captured.get(ClientIdentityFilter.CLIENT_KEY)).isEqualTo("app-android");
      assertThat(captured.get(ClientIdentityFilter.APP_VERSION_KEY)).isEqualTo("1.0.0");
    }

    @Test
    @DisplayName("로그를 오염시키는 문자는 제거한다")
    void doFilter_givenDirtyHeader_stripsUnsafeCharacters() throws Exception {
      // given
      MockHttpServletRequest request = new MockHttpServletRequest();
      request.addHeader("X-Client", "app\nINJECTED ERROR");

      // when
      Map<String, String> captured = captureDuringChain(request);

      // then
      assertThat(captured.get(ClientIdentityFilter.CLIENT_KEY)).isEqualTo("appINJECTEDERROR");
    }

    @Test
    @DisplayName("지나치게 긴 값은 잘라낸다")
    void doFilter_givenLongHeader_truncates() throws Exception {
      // given
      MockHttpServletRequest request = new MockHttpServletRequest();
      request.addHeader("X-Client", "a".repeat(100));

      // when
      Map<String, String> captured = captureDuringChain(request);

      // then
      assertThat(captured.get(ClientIdentityFilter.CLIENT_KEY)).hasSize(32);
    }
  }

  @Nested
  @DisplayName("헤더가 없을 때")
  class WithoutHeaders {

    @Test
    @DisplayName("unknown으로 채운다")
    void doFilter_givenNoHeaders_fillsUnknown() throws Exception {
      // when
      Map<String, String> captured = captureDuringChain(new MockHttpServletRequest());

      // then
      assertThat(captured.get(ClientIdentityFilter.CLIENT_KEY)).isEqualTo("unknown");
      assertThat(captured.get(ClientIdentityFilter.APP_VERSION_KEY)).isEqualTo("unknown");
    }

    @Test
    @DisplayName("빈 문자열도 unknown으로 본다")
    void doFilter_givenBlankHeader_fillsUnknown() throws Exception {
      // given
      MockHttpServletRequest request = new MockHttpServletRequest();
      request.addHeader("X-Client", "   ");

      // when
      Map<String, String> captured = captureDuringChain(request);

      // then
      assertThat(captured.get(ClientIdentityFilter.CLIENT_KEY)).isEqualTo("unknown");
    }
  }

  @Nested
  @DisplayName("MDC 정리")
  class Cleanup {

    @Test
    @DisplayName("요청이 끝나면 MDC를 비워 다음 요청에 새지 않게 한다")
    void doFilter_afterChain_clearsMdc() throws Exception {
      // given
      MockHttpServletRequest request = new MockHttpServletRequest();
      request.addHeader("X-Client", "app-android");

      // when
      captureDuringChain(request);

      // then
      assertThat(MDC.get(ClientIdentityFilter.CLIENT_KEY)).isNull();
      assertThat(MDC.get(ClientIdentityFilter.APP_VERSION_KEY)).isNull();
    }

    @Test
    @DisplayName("체인이 예외를 던져도 MDC를 비운다")
    void doFilter_whenChainThrows_stillClearsMdc() {
      // given
      MockHttpServletRequest request = new MockHttpServletRequest();
      request.addHeader("X-Client", "app-android");
      FilterChain throwing = (req, res) -> {
        throw new IllegalStateException("boom");
      };

      // when
      try {
        filter.doFilter(request, new MockHttpServletResponse(), throwing);
      } catch (Exception ignored) {
        // 전파는 이 테스트의 관심사가 아니다
      }

      // then
      assertThat(MDC.get(ClientIdentityFilter.CLIENT_KEY)).isNull();
    }
  }
}
