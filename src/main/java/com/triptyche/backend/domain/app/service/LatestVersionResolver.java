package com.triptyche.backend.domain.app.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.triptyche.backend.global.config.AppConfigProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/*
 * 최신 앱 버전을 배포 매니페스트(version.json)에서 읽는다.
 *
 * '무엇이 최신 빌드인가'는 APK를 올린 배포 파이프라인만 아는 사실이다. 이를 서버 환경변수로 두면
 * 릴리스마다 사람이 손으로 맞춰야 하고, 실제로 어긋난 채 방치됐다(서버 1.0.0 / 배포된 APK 1.0.1).
 * 그래서 CI가 APK와 함께 올리는 version.json을 진실의 원천으로 삼는다.
 *
 * 반대로 강제 업데이트 하한(min-supported-version)과 다운로드 주소는 여기서 읽지 않는다.
 * 그 둘은 '치명적 버그라 구버전을 급히 막는다' 같은 판단이 필요한 정책이라 서버 설정으로 남긴다.
 *
 * 이 조회가 실패해도 앱은 떠야 하므로, 어떤 실패든 기존 설정값으로 조용히 되돌아간다.
 */
@Slf4j
@Component
public class LatestVersionResolver {

  // 앱 실행마다 호출되는 엔드포인트라 매번 외부로 나가지 않도록 캐시한다.
  private static final Duration SUCCESS_TTL = Duration.ofMinutes(5);

  // 실패도 캐시한다. 매니페스트가 없거나 OCI가 죽었을 때 요청마다 타임아웃을 기다리면
  // /v1/app/config 전체가 느려지고, 그 지연이 앱 첫 화면을 붙잡는다.
  private static final Duration FAILURE_TTL = Duration.ofMinutes(1);

  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
  private static final Duration READ_TIMEOUT = Duration.ofSeconds(2);

  // 1.2.3 같은 점 구분 숫자만 허용한다. 매니페스트가 오염돼도 이상한 값이 앱까지 흘러가지 않게 한다.
  private static final Pattern VERSION_FORMAT = Pattern.compile("\\d{1,4}(\\.\\d{1,4}){0,3}");

  private final AppConfigProperties properties;
  private final RestClient restClient;
  private final Clock clock;
  private final AtomicReference<CachedVersion> cache = new AtomicReference<>();

  // 생성자가 둘이라 Spring이 스스로 고르지 못한다. 지정하지 않으면 기본 생성자를 찾다 기동에 실패한다.
  @Autowired
  public LatestVersionResolver(AppConfigProperties properties, RestClient.Builder builder) {
    this(properties, builder.requestFactory(timeoutRequestFactory()), Clock.systemUTC());
  }

  // 테스트에서 MockRestServiceServer와 고정 시계를 끼워 넣기 위한 통로.
  LatestVersionResolver(AppConfigProperties properties, RestClient.Builder builder, Clock clock) {
    this.properties = properties;
    this.restClient = builder.build();
    this.clock = clock;
  }

  private static SimpleClientHttpRequestFactory timeoutRequestFactory() {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(CONNECT_TIMEOUT);
    factory.setReadTimeout(READ_TIMEOUT);
    return factory;
  }

  public String resolve() {
    String fallback = properties.latestVersion();
    String manifestUrl = properties.versionManifestUrl();

    // 매니페스트 주소를 비워 두면 기존처럼 설정값만 쓴다(로컬·테스트 환경용).
    if (manifestUrl == null || manifestUrl.isBlank()) {
      return fallback;
    }

    CachedVersion cached = cache.get();
    if (cached != null && cached.isFresh(clock.instant())) {
      return cached.version();
    }

    try {
      VersionManifest manifest = restClient.get()
          .uri(manifestUrl)
          .retrieve()
          .body(VersionManifest.class);

      String latest = manifest == null ? null : manifest.latestVersion();
      if (latest == null || !VERSION_FORMAT.matcher(latest).matches()) {
        throw new IllegalStateException("매니페스트의 latestVersion이 올바르지 않다: " + latest);
      }

      cache.set(CachedVersion.of(latest, clock.instant(), SUCCESS_TTL));
      return latest;
    } catch (Exception e) {
      log.warn("최신 버전 매니페스트 조회 실패. 설정값 {} 로 대체한다. url={}, 원인={}",
          fallback, manifestUrl, e.toString());
      cache.set(CachedVersion.of(fallback, clock.instant(), FAILURE_TTL));
      return fallback;
    }
  }

  // CI가 필드를 추가해도 서버가 깨지지 않도록 모르는 필드는 무시한다.
  @JsonIgnoreProperties(ignoreUnknown = true)
  record VersionManifest(String latestVersion) {
  }

  private record CachedVersion(String version, Instant expiresAt) {

    static CachedVersion of(String version, Instant now, Duration ttl) {
      return new CachedVersion(version, now.plus(ttl));
    }

    boolean isFresh(Instant now) {
      return now.isBefore(expiresAt);
    }
  }
}
