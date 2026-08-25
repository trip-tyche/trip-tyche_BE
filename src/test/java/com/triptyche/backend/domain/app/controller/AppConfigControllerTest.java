package com.triptyche.backend.domain.app.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

  @Test
  @DisplayName("설정한 버전 정책이 응답에 그대로 실린다")
  void getAppConfig_givenConfiguredValues_returnsThem() throws Exception {
    // when & then
    mockMvc.perform(get("/v1/app/config"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.minSupportedVersion").value("1.2.0"))
            .andExpect(jsonPath("$.data.latestVersion").value("1.5.0"))
            .andExpect(jsonPath("$.data.updateUrl").value("https://example.com/download"));
  }
}
