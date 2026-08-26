package com.triptyche.backend.domain.app.service;

import com.triptyche.backend.global.config.AppConfigProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class LatestVersionResolverTest {

    private static final String MANIFEST_URL = "https://storage.example.com/app/version.json";
    private static final String CONFIGURED_FALLBACK = "1.0.0";

    private MutableClock clock;
    private RestClient.Builder builder;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
    }

    private LatestVersionResolver resolver(String manifestUrl) {
        AppConfigProperties properties =
                new AppConfigProperties("1.0.0", CONFIGURED_FALLBACK, "https://triptyche.cloud", manifestUrl);
        return new LatestVersionResolver(properties, builder, clock);
    }

    private void expectManifest(String body) {
        server.expect(requestTo(MANIFEST_URL))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }

    @Test
    void 매니페스트의_버전을_반환한다() {
        expectManifest("{\"latestVersion\":\"1.2.3\",\"latestVersionCode\":7}");

        assertThat(resolver(MANIFEST_URL).resolve()).isEqualTo("1.2.3");
        server.verify();
    }

    @Test
    void 조회에_실패하면_설정값으로_대체한다() {
        server.expect(requestTo(MANIFEST_URL)).andRespond(withServerError());

        assertThat(resolver(MANIFEST_URL).resolve()).isEqualTo(CONFIGURED_FALLBACK);
        server.verify();
    }

    @Test
    void 버전_형식이_아니면_설정값으로_대체한다() {
        // 매니페스트가 오염돼도 이상한 값이 앱까지 흘러가면 안 된다.
        expectManifest("{\"latestVersion\":\"latest\"}");

        assertThat(resolver(MANIFEST_URL).resolve()).isEqualTo(CONFIGURED_FALLBACK);
        server.verify();
    }

    @Test
    void 매니페스트_주소가_비면_조회하지_않는다() {
        // 요청 기대를 등록하지 않았으므로, 호출이 나가면 테스트가 깨진다.
        assertThat(resolver("").resolve()).isEqualTo(CONFIGURED_FALLBACK);
        server.verify();
    }

    @Test
    void 캐시가_살아있는_동안은_다시_조회하지_않는다() {
        expectManifest("{\"latestVersion\":\"1.2.3\"}");
        LatestVersionResolver resolver = resolver(MANIFEST_URL);

        assertThat(resolver.resolve()).isEqualTo("1.2.3");
        clock.advance(Duration.ofMinutes(4));
        assertThat(resolver.resolve()).isEqualTo("1.2.3");

        // 기대는 한 건만 등록했다 — 두 번 나갔다면 여기서 드러난다.
        server.verify();
    }

    @Test
    void 캐시가_만료되면_다시_조회한다() {
        expectManifest("{\"latestVersion\":\"1.2.3\"}");
        expectManifest("{\"latestVersion\":\"1.3.0\"}");
        LatestVersionResolver resolver = resolver(MANIFEST_URL);

        assertThat(resolver.resolve()).isEqualTo("1.2.3");
        clock.advance(Duration.ofMinutes(6));
        assertThat(resolver.resolve()).isEqualTo("1.3.0");

        server.verify();
    }

    @Test
    void 실패도_잠시_캐시해_요청마다_타임아웃을_기다리지_않는다() {
        server.expect(requestTo(MANIFEST_URL)).andRespond(withServerError());
        LatestVersionResolver resolver = resolver(MANIFEST_URL);

        assertThat(resolver.resolve()).isEqualTo(CONFIGURED_FALLBACK);
        clock.advance(Duration.ofSeconds(30));
        assertThat(resolver.resolve()).isEqualTo(CONFIGURED_FALLBACK);

        server.verify();
    }

    private static final class MutableClock extends Clock {

        private Instant now;

        private MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
