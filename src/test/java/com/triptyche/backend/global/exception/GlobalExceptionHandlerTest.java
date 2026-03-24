package com.triptyche.backend.global.exception;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.triptyche.backend.domain.share.controller.ShareController;
import com.triptyche.backend.domain.share.dto.ShareCreateRequestDTO;
import com.triptyche.backend.domain.share.service.ShareService;
import com.triptyche.backend.domain.user.model.User;
import com.triptyche.backend.domain.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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

      ShareCreateRequestDTO request = new ShareCreateRequestDTO("TRIP_KEY_ABC", 2L);

      // when & then
      mockMvc.perform(post("/v1/trips/share")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(objectMapper.writeValueAsString(request)))
              .andExpect(status().isConflict())
              .andExpect(jsonPath("$.code").value(9900));
    }
  }
}