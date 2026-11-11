package com.triptyche.backend.global.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SensitiveDataMaskerTest {

  @Nested
  @DisplayName("maskEmails")
  class MaskEmails {

    @Test
    @DisplayName("null과 빈 문자열은 그대로 돌려준다")
    void maskEmails_givenNullOrEmpty_returnsAsIs() {
      assertThat(SensitiveDataMasker.maskEmails(null)).isNull();
      assertThat(SensitiveDataMasker.maskEmails("")).isEmpty();
    }

    @Test
    @DisplayName("이메일이 없는 문자열은 변하지 않는다")
    void maskEmails_givenNoEmail_returnsUnchanged() {
      assertThat(SensitiveDataMasker.maskEmails("토큰 만료: JWT expired at 2026-11-11T00:00:00Z"))
              .isEqualTo("토큰 만료: JWT expired at 2026-11-11T00:00:00Z");
      assertThat(SensitiveDataMasker.maskEmails("@mention 과 a@b 는 이메일이 아니다"))
              .isEqualTo("@mention 과 a@b 는 이메일이 아니다");
    }

    @Test
    @DisplayName("문장 속 이메일을 마스킹하고 앞뒤 문맥은 유지한다")
    void maskEmails_givenEmailInSentence_masksOnlyTheEmail() {
      assertThat(SensitiveDataMasker.maskEmails("사용자 test@example.com 로그인 실패"))
              .isEqualTo("사용자 [EMAIL] 로그인 실패");
      assertThat(SensitiveDataMasker.maskEmails("refresh token not found (user=test@example.com)."))
              .isEqualTo("refresh token not found (user=[EMAIL]).");
    }

    @Test
    @DisplayName("점·플러스·하위 도메인이 섞인 이메일도 마스킹한다")
    void maskEmails_givenComplexEmail_masksWholeAddress() {
      assertThat(SensitiveDataMasker.maskEmails("first.last+tag@mail.example.co.kr"))
              .isEqualTo("[EMAIL]");
    }

    @Test
    @DisplayName("한 문자열의 이메일을 모두 마스킹한다")
    void maskEmails_givenMultipleEmails_masksAll() {
      assertThat(SensitiveDataMasker.maskEmails("from a@x.com to b@y.org"))
              .isEqualTo("from [EMAIL] to [EMAIL]");
    }

    @Test
    @DisplayName("URL 인코딩된 @(%40) 이메일도 마스킹한다")
    void maskEmails_givenUrlEncodedAt_masksIt() {
      assertThat(SensitiveDataMasker.maskEmails("a%40b.com")).isEqualTo("[EMAIL]");
      assertThat(SensitiveDataMasker.maskEmails("/v1/users/first.last%2Btag%40mail.example.co.kr/trips"))
              .isEqualTo("/v1/users/[EMAIL]/trips");
      assertThat(SensitiveDataMasker.maskEmails("from a%40x.com to b@y.org")).isEqualTo("from [EMAIL] to [EMAIL]");
    }

    @Test
    @DisplayName("도메인이 없는 %40은 이메일로 보지 않는다")
    void maskEmails_givenPercent40WithoutDomain_returnsUnchanged() {
      assertThat(SensitiveDataMasker.maskEmails("100%40 할인, %40mention")).isEqualTo("100%40 할인, %40mention");
    }

    @Test
    @DisplayName("%가 많은 긴 문자열도 빠르게 처리한다")
    void maskEmails_givenVeryLongTextWithPercent_finishesQuickly() {
      String longText = "a%41,".repeat(100_000) + "%40";

      assertTimeoutPreemptively(Duration.ofSeconds(2), () -> SensitiveDataMasker.maskEmails(longText));
    }

    @Test
    @DisplayName("@가 없는 긴 문자열도 빠르게 처리한다")
    void maskEmails_givenVeryLongTextWithoutAt_finishesQuickly() {
      String longText = "a.".repeat(100_000) + "@";

      assertTimeoutPreemptively(Duration.ofSeconds(2), () -> SensitiveDataMasker.maskEmails(longText));
    }
  }

  @Nested
  @DisplayName("maskValue")
  class MaskValue {

    @Test
    @DisplayName("Map과 List 안의 문자열까지 재귀적으로 마스킹한다")
    void maskValue_givenNestedStructure_masksStringsRecursively() {
      Map<String, Object> input = Map.of(
              "user", "test@example.com",
              "list", List.of("a@b.com", "plain"),
              "count", 3);

      Object masked = SensitiveDataMasker.maskValue(input);

      assertThat(masked).isInstanceOf(Map.class);
      @SuppressWarnings("unchecked")
      Map<String, Object> result = (Map<String, Object>) masked;
      assertThat(result.get("user")).isEqualTo("[EMAIL]");
      assertThat(result.get("list")).isEqualTo(List.of("[EMAIL]", "plain"));
      assertThat(result.get("count")).isEqualTo(3);
    }

    @Test
    @DisplayName("원본 Map은 수정하지 않는다")
    void maskValue_givenMap_doesNotMutateOriginal() {
      Map<String, Object> input = Map.of("user", "test@example.com");

      SensitiveDataMasker.maskValue(input);

      assertThat(input.get("user")).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("문자열이 아닌 값과 null은 그대로 돌려준다")
    void maskValue_givenNonString_returnsSameInstance() {
      Object number = 42;

      assertThat(SensitiveDataMasker.maskValue(number)).isSameAs(number);
      assertThat(SensitiveDataMasker.maskValue(null)).isNull();
    }
  }

  @Nested
  @DisplayName("isSensitiveHeader")
  class IsSensitiveHeader {

    @Test
    @DisplayName("Authorization · Cookie · Set-Cookie는 대소문자와 무관하게 민감 헤더다")
    void isSensitiveHeader_givenAuthHeaders_returnsTrue() {
      assertThat(SensitiveDataMasker.isSensitiveHeader("Authorization")).isTrue();
      assertThat(SensitiveDataMasker.isSensitiveHeader("COOKIE")).isTrue();
      assertThat(SensitiveDataMasker.isSensitiveHeader("set-cookie")).isTrue();
    }

    @Test
    @DisplayName("그 외 헤더와 null은 민감 헤더가 아니다")
    void isSensitiveHeader_givenOtherHeader_returnsFalse() {
      assertThat(SensitiveDataMasker.isSensitiveHeader("User-Agent")).isFalse();
      assertThat(SensitiveDataMasker.isSensitiveHeader(null)).isFalse();
    }
  }

  @Nested
  @DisplayName("sanitizeQueryString")
  class SanitizeQueryString {

    @Test
    @DisplayName("token과 code 파라미터를 제거하고 나머지는 유지한다")
    void sanitizeQueryString_givenTokenAndCode_removesThem() {
      assertThat(SensitiveDataMasker.sanitizeQueryString("token=abc.def.ghi&page=2&code=xyz&size=10"))
              .isEqualTo("page=2&size=10");
    }

    @Test
    @DisplayName("파라미터 이름은 대소문자와 URL 인코딩을 무시하고 비교한다")
    void sanitizeQueryString_givenEncodedOrUpperCaseName_removesIt() {
      assertThat(SensitiveDataMasker.sanitizeQueryString("TOKEN=a&%63ode=b&keep=1")).isEqualTo("keep=1");
    }

    @Test
    @DisplayName("값이 없는 token 파라미터도 제거한다")
    void sanitizeQueryString_givenTokenWithoutValue_removesIt() {
      assertThat(SensitiveDataMasker.sanitizeQueryString("token&a=1")).isEqualTo("a=1");
    }

    @Test
    @DisplayName("이름만 비슷한 파라미터는 제거하지 않는다")
    void sanitizeQueryString_givenSimilarName_keepsIt() {
      assertThat(SensitiveDataMasker.sanitizeQueryString("tokenType=bearer&zipcode=123"))
              .isEqualTo("tokenType=bearer&zipcode=123");
    }

    @Test
    @DisplayName("남은 값의 이메일은 마스킹한다")
    void sanitizeQueryString_givenEmailValue_masksIt() {
      assertThat(SensitiveDataMasker.sanitizeQueryString("email=test@example.com&token=abc"))
              .isEqualTo("email=[EMAIL]");
    }

    @Test
    @DisplayName("URL 인코딩된 이메일 값도 마스킹한다")
    void sanitizeQueryString_givenEncodedEmailValue_masksIt() {
      assertThat(SensitiveDataMasker.sanitizeQueryString("email=test%40example.com&page=1"))
              .isEqualTo("email=[EMAIL]&page=1");
    }

    @Test
    @DisplayName("전부 제거되면 빈 문자열, null과 빈 문자열은 그대로 돌려준다")
    void sanitizeQueryString_givenEdgeCases_returnsExpected() {
      assertThat(SensitiveDataMasker.sanitizeQueryString("token=abc")).isEmpty();
      assertThat(SensitiveDataMasker.sanitizeQueryString(null)).isNull();
      assertThat(SensitiveDataMasker.sanitizeQueryString("")).isEmpty();
    }
  }
}
