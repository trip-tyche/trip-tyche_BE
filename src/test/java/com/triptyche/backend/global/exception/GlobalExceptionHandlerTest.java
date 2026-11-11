package com.triptyche.backend.global.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.triptyche.backend.domain.share.controller.ShareController;
import com.triptyche.backend.domain.share.dto.ShareCreateRequest;
import com.triptyche.backend.domain.share.service.ShareService;
import com.triptyche.backend.domain.user.model.User;
import com.triptyche.backend.domain.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ShareController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class GlobalExceptionHandlerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private ShareService shareService;

  @MockBean
  private UserRepository userRepository;

  private ObjectMapper objectMapper;

  private static final String TEST_EMAIL = "test@example.com";

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();

    User mockUser = User.builder()
            .userId(1L)
            .userName("테스트유저")
            .userNickName("닉네임")
            .userEmail(TEST_EMAIL)
            .provider("google")
            .build();

    given(userRepository.findByUserEmail(TEST_EMAIL)).willReturn(Optional.of(mockUser));
  }

  @Nested
  @DisplayName("DataIntegrityViolationException 처리")
  class HandleDataIntegrityViolation {

    @Test
    @WithMockUser(username = TEST_EMAIL)
    @DisplayName("DataIntegrityViolationException 발생 시 409 응답과 code 9900을 반환한다")
    void handleDataIntegrityViolation_returns409WithCode9900() throws Exception {
      // given
      given(shareService.createShare(any(), any())).willThrow(DataIntegrityViolationException.class);

      ShareCreateRequest request = new ShareCreateRequest("TRIP_KEY_ABC", 2L);

      // when & then
      mockMvc.perform(post("/v1/trips/share")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(objectMapper.writeValueAsString(request)))
              .andExpect(status().isConflict())
              .andExpect(jsonPath("$.code").value(9900));
    }
  }

  @Nested
  @DisplayName("NoResourceFoundException 처리")
  class HandleNoResourceFound {

    private final Logger handlerLogger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void attachLogAppender() {
      logAppender = new ListAppender<>();
      logAppender.start();
      handlerLogger.addAppender(logAppender);
    }

    @AfterEach
    void detachLogAppender() {
      handlerLogger.detachAppender(logAppender);
    }

    @Test
    @DisplayName("존재하지 않는 경로를 요청하면 404 응답과 code 1007을 반환한다")
    void handleNoResourceFound_returns404WithCode1007() throws Exception {
      // when & then
      mockMvc.perform(get("/wp-admin/setup-config.php"))
              .andExpect(status().isNotFound())
              .andExpect(jsonPath("$.code").value(1007));
    }

    @Test
    @DisplayName("존재하지 않는 경로를 요청해도 에러 로그를 남기지 않는다")
    void handleNoResourceFound_doesNotLogError() throws Exception {
      // when
      mockMvc.perform(get("/actuator/env")).andExpect(status().isNotFound());

      // then
      assertThat(logAppender.list)
              .noneMatch(event -> event.getLevel().isGreaterOrEqual(Level.ERROR));
    }
  }
}
