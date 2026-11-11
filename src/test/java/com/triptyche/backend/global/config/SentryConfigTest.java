package com.triptyche.backend.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.sentry.Breadcrumb;
import io.sentry.CustomSamplingContext;
import io.sentry.Hint;
import io.sentry.SamplingContext;
import io.sentry.SentryEvent;
import io.sentry.SentryOptions;
import io.sentry.TransactionContext;
import io.sentry.protocol.Message;
import io.sentry.protocol.Request;
import io.sentry.protocol.SentryException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class SentryConfigTest {

  private final SentryConfig config = new SentryConfig();

  @Nested
  @DisplayName("traces 샘플러")
  class TracesSampler {

    private final SentryOptions.TracesSamplerCallback sampler = config.sentryTracesSamplerCallback();

    @SuppressWarnings("deprecation")
    private SamplingContext samplingContextWith(Object request) {
      CustomSamplingContext custom = new CustomSamplingContext();
      custom.set(SentryConfig.SAMPLING_REQUEST_KEY, request);
      return new SamplingContext(new TransactionContext("test", "http.server"), custom);
    }

    private MockHttpServletRequest request(String uri, String userAgent) {
      MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
      if (userAgent != null) {
        request.addHeader("User-Agent", userAgent);
      }
      return request;
    }

    @Test
    @DisplayName("일반 API 요청은 전부 수집한다")
    void sample_givenNormalApiRequest_returnsOne() {
      Double rate = sampler.sample(samplingContextWith(request("/v1/trips", "Mozilla/5.0 (Linux; Android 14)")));

      assertThat(rate).isEqualTo(1.0);
    }

    @Test
    @DisplayName("User-Agent가 없어도 수집한다")
    void sample_givenNoUserAgent_returnsOne() {
      Double rate = sampler.sample(samplingContextWith(request("/v1/trips", null)));

      assertThat(rate).isEqualTo(1.0);
    }

    @Test
    @DisplayName("/actuator 요청은 수집하지 않는다")
    void sample_givenActuatorRequest_returnsZero() {
      assertThat(sampler.sample(samplingContextWith(request("/actuator/health", "curl/8.0")))).isEqualTo(0.0);
      assertThat(sampler.sample(samplingContextWith(request("/actuator/prometheus", "Prometheus/2.53")))).isEqualTo(0.0);
    }

    @Test
    @DisplayName("k6 부하 테스트 요청은 수집하지 않는다")
    void sample_givenK6UserAgent_returnsZero() {
      assertThat(sampler.sample(samplingContextWith(request("/v1/trips", "k6/0.50.0 (https://k6.io/)")))).isEqualTo(0.0);
      assertThat(sampler.sample(samplingContextWith(request("/v1/trips", "K6/0.50.0")))).isEqualTo(0.0);
    }

    @Test
    @SuppressWarnings("deprecation")
    @DisplayName("요청 정보가 없는 트랜잭션은 수집한다")
    void sample_givenNoRequestInContext_returnsOne() {
      SamplingContext withoutCustomContext =
              new SamplingContext(new TransactionContext("job", "task"), null);

      assertThat(sampler.sample(withoutCustomContext)).isEqualTo(1.0);
      assertThat(sampler.sample(samplingContextWith("not-a-request"))).isEqualTo(1.0);
    }
  }

  @Nested
  @DisplayName("beforeSend 마스킹")
  class BeforeSend {

    private final SentryOptions.BeforeSendCallback beforeSend = config.sentryBeforeSendCallback();

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
      return (Map<String, Object>) value;
    }

    @Test
    @DisplayName("메시지와 예외 값의 이메일을 마스킹한다")
    void execute_givenEmailInMessageAndException_masksThem() {
      SentryEvent event = new SentryEvent();
      Message message = new Message();
      message.setMessage("리프레시 토큰 없음: {}");
      message.setFormatted("리프레시 토큰 없음: test@example.com");
      message.setParams(List.of("test@example.com"));
      event.setMessage(message);
      SentryException exception = new SentryException();
      exception.setValue("User not found: test@example.com");
      event.setExceptions(List.of(exception));

      SentryEvent result = beforeSend.execute(event, new Hint());

      assertThat(result).isNotNull();
      assertThat(result.getMessage().getFormatted()).isEqualTo("리프레시 토큰 없음: [EMAIL]");
      assertThat(result.getMessage().getParams()).containsExactly("[EMAIL]");
      assertThat(result.getExceptions().get(0).getValue()).isEqualTo("User not found: [EMAIL]");
    }

    @Test
    @DisplayName("extra · contexts · tags 안의 이메일도 마스킹한다")
    void execute_givenEmailInExtrasContextsTags_masksThem() {
      SentryEvent event = new SentryEvent();
      event.setExtra("user", "test@example.com");
      event.setExtra("nested", new HashMap<>(Map.of("email", "a@b.com", "count", 1)));
      event.getContexts().set("custom", new HashMap<>(Map.of("owner", "owner@example.com")));
      event.setTag("note", "from test@example.com");

      SentryEvent result = beforeSend.execute(event, new Hint());

      assertThat(result.getExtra("user")).isEqualTo("[EMAIL]");
      assertThat(asMap(result.getExtra("nested"))).containsEntry("email", "[EMAIL]").containsEntry("count", 1);
      assertThat(asMap(result.getContexts().get("custom"))).containsEntry("owner", "[EMAIL]");
      assertThat(result.getTag("note")).isEqualTo("from [EMAIL]");
    }

    @Test
    @DisplayName("Authorization · Cookie · Set-Cookie 헤더와 쿠키 값을 제거한다")
    void execute_givenAuthHeaders_dropsThem() {
      SentryEvent event = new SentryEvent();
      Request request = new Request();
      Map<String, String> headers = new HashMap<>();
      headers.put("Authorization", "Bearer secret");
      headers.put("cookie", "access_token=secret");
      headers.put("Set-Cookie", "access_token=secret");
      headers.put("User-Agent", "Mozilla/5.0");
      request.setHeaders(headers);
      request.setCookies("access_token=secret");
      event.setRequest(request);

      SentryEvent result = beforeSend.execute(event, new Hint());

      assertThat(result.getRequest().getHeaders()).containsOnlyKeys("User-Agent");
      assertThat(result.getRequest().getCookies()).isNull();
    }

    @Test
    @DisplayName("쿼리 스트링에서 token · code를 제거하고 URL의 이메일을 마스킹한다")
    void execute_givenSensitiveQuery_sanitizesRequest() {
      SentryEvent event = new SentryEvent();
      Request request = new Request();
      request.setUrl("https://triptyche.cloud/v1/users/test@example.com");
      request.setQueryString("token=abc&code=xyz&page=1");
      event.setRequest(request);

      SentryEvent result = beforeSend.execute(event, new Hint());

      assertThat(result.getRequest().getUrl()).isEqualTo("https://triptyche.cloud/v1/users/[EMAIL]");
      assertThat(result.getRequest().getQueryString()).isEqualTo("page=1");
    }

    @Test
    @DisplayName("메시지·예외·요청이 없는 이벤트도 그대로 통과시킨다")
    void execute_givenEmptyEvent_returnsEvent() {
      SentryEvent event = new SentryEvent();

      assertThat(beforeSend.execute(event, new Hint())).isSameAs(event);
    }
  }

  @Nested
  @DisplayName("beforeBreadcrumb 마스킹")
  class BeforeBreadcrumb {

    private final SentryOptions.BeforeBreadcrumbCallback beforeBreadcrumb = config.sentryBeforeBreadcrumbCallback();

    @Test
    @DisplayName("브레드크럼 메시지와 data의 이메일을 마스킹한다")
    void execute_givenEmailInBreadcrumb_masksMessageAndData() {
      Breadcrumb breadcrumb = new Breadcrumb("refresh 실패: test@example.com");
      breadcrumb.setData("user", "test@example.com");
      breadcrumb.setData("attempt", 2);

      Breadcrumb result = beforeBreadcrumb.execute(breadcrumb, new Hint());

      assertThat(result).isNotNull();
      assertThat(result.getMessage()).isEqualTo("refresh 실패: [EMAIL]");
      assertThat(result.getData("user")).isEqualTo("[EMAIL]");
      assertThat(result.getData("attempt")).isEqualTo(2);
    }

    @Test
    @DisplayName("data가 없는 브레드크럼도 처리한다")
    void execute_givenBreadcrumbWithoutData_returnsBreadcrumb() {
      Breadcrumb breadcrumb = new Breadcrumb("plain message");

      assertThat(beforeBreadcrumb.execute(breadcrumb, new Hint()).getMessage()).isEqualTo("plain message");
    }
  }
}
