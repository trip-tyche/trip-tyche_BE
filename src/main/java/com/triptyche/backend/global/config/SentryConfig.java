package com.triptyche.backend.global.config;

import com.triptyche.backend.global.util.SensitiveDataMasker;
import io.sentry.Breadcrumb;
import io.sentry.CustomSamplingContext;
import io.sentry.SentryBaseEvent;
import io.sentry.SentryEvent;
import io.sentry.SentryOptions;
import io.sentry.protocol.Message;
import io.sentry.protocol.Request;
import io.sentry.protocol.SentryException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Sentry 전송 직전의 개인정보 제거와 traces 샘플링 정책.
 * sentry-spring-boot-starter가 이 빈들을 SentryOptions에 자동으로 연결한다.
 * SENTRY_DSN이 비어 있으면 SDK 자체가 꺼지므로 이 빈들은 호출되지 않는다.
 */
@Configuration
public class SentryConfig {

  // SentryTracingFilter가 CustomSamplingContext에 HttpServletRequest를 넣는 키
  static final String SAMPLING_REQUEST_KEY = "request";

  private static final String ACTUATOR_PATH_PREFIX = "/actuator";
  // k6 기본 User-Agent는 "k6/<version> (https://k6.io/)". 슬래시까지 맞춰야 "Redmi K60" 같은 기기명이 걸리지 않는다.
  private static final String LOAD_TEST_USER_AGENT_MARKER = "k6/";

  @Bean
  public SentryOptions.BeforeSendCallback sentryBeforeSendCallback() {
    return (event, hint) -> {
      scrubEvent(event);
      return event;
    };
  }

  /**
   * JWTAuthenticationFilter가 WebSocket 핸드셰이크의 ?token= 을 받으므로 트랜잭션의 request도 이벤트와 똑같이 걸러야 한다.
   * SentryTransaction에는 이름 setter가 없어 이메일이 든 이름은 마스킹하지 못하고, 해당 트랜잭션을 버린다.
   */
  @Bean
  public SentryOptions.BeforeSendTransactionCallback sentryBeforeSendTransactionCallback() {
    return (transaction, hint) -> {
      String name = transaction.getTransaction();
      if (name != null && !name.equals(SensitiveDataMasker.maskEmails(name))) {
        return null;
      }
      scrubBaseEvent(transaction);
      return transaction;
    };
  }

  @Bean
  public SentryOptions.BeforeBreadcrumbCallback sentryBeforeBreadcrumbCallback() {
    return (breadcrumb, hint) -> {
      scrubBreadcrumb(breadcrumb);
      return breadcrumb;
    };
  }

  /**
   * /actuator 호출(healthcheck · Prometheus 수집)과 k6 부하 테스트는 쿼터를 쓰지 않도록 0으로,
   * 나머지는 전부 수집(1.0)한다. 요청 정보가 없는 트랜잭션도 1.0이다.
   */
  @Bean
  public SentryOptions.TracesSamplerCallback sentryTracesSamplerCallback() {
    return samplingContext -> {
      HttpServletRequest request = extractRequest(samplingContext.getCustomSamplingContext());
      if (request == null) {
        return 1.0;
      }
      return isIgnoredForTracing(request) ? 0.0 : 1.0;
    };
  }

  private static HttpServletRequest extractRequest(CustomSamplingContext context) {
    if (context == null) {
      return null;
    }
    return context.get(SAMPLING_REQUEST_KEY) instanceof HttpServletRequest request ? request : null;
  }

  private static boolean isIgnoredForTracing(HttpServletRequest request) {
    String uri = request.getRequestURI();
    if (uri != null && uri.startsWith(ACTUATOR_PATH_PREFIX)) {
      return true;
    }
    String userAgent = request.getHeader("User-Agent");
    return userAgent != null && userAgent.toLowerCase(Locale.ROOT).contains(LOAD_TEST_USER_AGENT_MARKER);
  }

  static void scrubEvent(SentryEvent event) {
    Message message = event.getMessage();
    if (message != null) {
      message.setMessage(SensitiveDataMasker.maskEmails(message.getMessage()));
      message.setFormatted(SensitiveDataMasker.maskEmails(message.getFormatted()));
      if (message.getParams() != null) {
        message.setParams(message.getParams().stream().map(SensitiveDataMasker::maskEmails).toList());
      }
    }

    List<SentryException> exceptions = event.getExceptions();
    if (exceptions != null) {
      exceptions.forEach(e -> e.setValue(SensitiveDataMasker.maskEmails(e.getValue())));
    }

    scrubBaseEvent(event);
  }

  private static void scrubBaseEvent(SentryBaseEvent event) {
    maskExtras(event);
    maskContexts(event);
    maskTags(event);
    scrubRequest(event.getRequest());
  }

  static void scrubBreadcrumb(Breadcrumb breadcrumb) {
    breadcrumb.setMessage(SensitiveDataMasker.maskEmails(breadcrumb.getMessage()));
    // 순회 중 수정하지 않도록 복사본을 돈다.
    new ArrayList<>(breadcrumb.getData().entrySet())
        .forEach(entry -> breadcrumb.setData(entry.getKey(), SensitiveDataMasker.maskValue(entry.getValue())));
  }

  private static void maskExtras(SentryBaseEvent event) {
    Map<String, Object> extras = event.getExtras();
    if (extras == null) {
      return;
    }
    new ArrayList<>(extras.entrySet())
        .forEach(entry -> event.setExtra(entry.getKey(), SensitiveDataMasker.maskValue(entry.getValue())));
  }

  // Contexts에는 App · Device 같은 타입 객체도 들어 있으므로 문자열과 Map 값만 건드린다.
  private static void maskContexts(SentryBaseEvent event) {
    new ArrayList<>(event.getContexts().entrySet())
        .forEach(entry -> event.getContexts().set(entry.getKey(), SensitiveDataMasker.maskValue(entry.getValue())));
  }

  private static void maskTags(SentryBaseEvent event) {
    Map<String, String> tags = event.getTags();
    if (tags == null) {
      return;
    }
    new ArrayList<>(tags.entrySet())
        .forEach(entry -> event.setTag(entry.getKey(), SensitiveDataMasker.maskEmails(entry.getValue())));
  }

  private static void scrubRequest(Request request) {
    if (request == null) {
      return;
    }
    request.setCookies(null);
    if (request.getHeaders() != null) {
      request.getHeaders().keySet().removeIf(SensitiveDataMasker::isSensitiveHeader);
    }
    request.setUrl(SensitiveDataMasker.maskEmails(request.getUrl()));
    request.setQueryString(SensitiveDataMasker.sanitizeQueryString(request.getQueryString()));
    if (request.getData() != null) {
      request.setData(SensitiveDataMasker.maskValue(request.getData()));
    }
  }
}
