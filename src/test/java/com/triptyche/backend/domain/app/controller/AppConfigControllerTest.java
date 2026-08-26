package com.triptyche.backend.domain.app.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.triptyche.backend.domain.app.service.LatestVersionResolver;
import com.triptyche.backend.domain.user.repository.UserRepository;
import com.triptyche.backend.global.config.AppConfigProperties;
import com.triptyche.backend.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AppConfigController.class)
@Import(GlobalExceptionHandler.class)
@EnableConfigurationProperties(AppConfigProperties.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
        "app.min-supported-version=1.2.0",
        "app.latest-version=1.5.0",
        "app.update-url=https://example.com/download"
})
class AppConfigControllerTest {

  @Autowired
  private MockMvc mockMvc;

  // CurrentUserArgumentResolver가 내부적으로 UserRepository를 사용하므로 MockBean 등록 필요
  @MockBean
  private UserRepository userRepository;

  // 최신 버전은 이제 설정값이 아니라 배포 매니페스트에서 온다.
  @MockBean
  private LatestVersionResolver latestVersionResolver;

  @Test
  @DisplayName("설정한 버전 정책이 응답에 그대로 실린다")
  void getAppConfig_givenConfiguredValues_returnsThem() throws Exception {
    // given
    given(latestVersionResolver.resolve()).willReturn("1.5.0");

    // when & then
    mockMvc.perform(get("/v1/app/config"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.minSupportedVersion").value("1.2.0"))
            .andExpect(jsonPath("$.data.latestVersion").value("1.5.0"))
            .andExpect(jsonPath("$.data.updateUrl").value("https://example.com/download"));
  }

  @Test
  @DisplayName("최신 버전은 설정값이 아니라 리졸버가 정한 값이 실린다")
  void getAppConfig_latestVersionComesFromResolver() throws Exception {
    // given — app.latest-version은 1.5.0이지만 매니페스트가 더 최신을 알고 있다
    given(latestVersionResolver.resolve()).willReturn("1.9.0");

    // when & then
    mockMvc.perform(get("/v1/app/config"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.latestVersion").value("1.9.0"));
  }
}
