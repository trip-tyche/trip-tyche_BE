package com.triptyche.backend.domain.trip.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.triptyche.backend.domain.trip.dto.TripCreateResponse;
import com.triptyche.backend.domain.trip.dto.TripUpdateRequest;
import com.triptyche.backend.domain.trip.service.TripCommandService;
import com.triptyche.backend.domain.user.model.User;
import com.triptyche.backend.domain.user.repository.UserRepository;
import com.triptyche.backend.global.common.ResultCode;
import com.triptyche.backend.global.exception.CustomException;
import com.triptyche.backend.global.exception.GlobalExceptionHandler;
import java.time.LocalDate;
import java.util.List;
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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TripCommandController.class)
@Import({GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class TripCommandControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private TripCommandService tripCommandService;

  // CurrentUserArgumentResolver가 내부적으로 UserRepository를 사용하므로 MockBean 등록 필요
  @MockBean
  private UserRepository userRepository;

  private ObjectMapper objectMapper;
  private User mockUser;
  private static final String TEST_EMAIL = "test@example.com";
  private static final String TEST_TRIP_KEY = "TRIP_KEY_ABC";

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());

    mockUser = User.builder()
            .userId(1L)
            .userName("테스트유저")
            .userNickName("닉네임")
            .userEmail(TEST_EMAIL)
            .provider("google")
            .build();

    // CurrentUserArgumentResolver: SecurityContext의 이메일로 User 조회
    given(userRepository.findByUserEmail(TEST_EMAIL))
            .willReturn(Optional.of(mockUser));
  }

  @Nested
  @DisplayName("POST /v1/trips — tripId 임시 생성")
  class CreateTrip {

    @Test
    @WithMockUser(username = TEST_EMAIL)
    @DisplayName("인증된 사용자가 요청하면 tripKey가 포함된 200 응답을 반환한다")
    void createTrip_givenAuthenticatedUser_returns200WithTripKey() throws Exception {
      //given
      TripCreateResponse response = new TripCreateResponse("ABCD1234");
      given(tripCommandService.createTripId(any(User.class))).willReturn(response);

      //when & then
      mockMvc.perform(post("/v1/trips")
                      .contentType(MediaType.APPLICATION_JSON))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.data.tripKey").value("ABCD1234"));
    }


    @Nested
    @DisplayName("PATCH /v1/trips/{tripKey}/images-uploaded — 이미지 업로드 완료 상태 변경")
    class MarkImagesUploaded {

      @Test
      @WithMockUser(username = TEST_EMAIL)
      @DisplayName("유효한 tripKey로 요청하면 상태 변경 성공 메시지와 200을 반환한다")
      void markImagesUploaded_givenValidTripKey_returns200() throws Exception {
        //given
        willDoNothing().given(tripCommandService).markImagesUploaded(any(User.class), eq(TEST_TRIP_KEY));

        //when & then
        mockMvc.perform(patch("/v1/trips/{tripKey}/images-uploaded", TEST_TRIP_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("이미지 업로드 완료 상태로 변경되었습니다."));
      }

      @Test
      @WithMockUser(username = TEST_EMAIL)
      @DisplayName("존재하지 않는 tripKey로 요청하면 403 응답을 반환한다")
      void markImagesUploaded_givenInvalidTripKey_returns403() throws Exception {
        //given
        willThrow(new CustomException(ResultCode.ACCESS_DENIED))
                .given(tripCommandService).markImagesUploaded(any(User.class), eq("INVALID_KEY"));

        //when & then
        mockMvc.perform(patch("/v1/trips/{tripKey}/images-uploaded", "INVALID_KEY"))
                .andExpect(status().isForbidden());
      }

      @Test
      @WithMockUser(username = TEST_EMAIL)
      @DisplayName("접근 권한이 없는 여행에 요청하면 403 응답을 반환한다")
      void markImagesUploaded_givenUnauthorizedUser_returns403() throws Exception {
        //given
        willThrow(new CustomException(ResultCode.ACCESS_DENIED))
                .given(tripCommandService).markImagesUploaded(any(User.class), eq(TEST_TRIP_KEY));

        //when & then
        mockMvc.perform(patch("/v1/trips/{tripKey}/images-uploaded", TEST_TRIP_KEY))
                .andExpect(status().isForbidden());
      }
    }

    @Nested
    @DisplayName("PATCH /v1/trips/{tripKey}/finalize — 여행 최종 확정")
    class FinalizeTrip {

      @Test
      @WithMockUser(username = TEST_EMAIL)
      @DisplayName("유효한 tripKey로 요청하면 여행 확정 성공 메시지와 200을 반환한다")
      void finalizeTrip_givenValidTripKey_returns200() throws Exception {
        //given
        willDoNothing().given(tripCommandService).finalizeTrip(any(User.class), eq(TEST_TRIP_KEY));

        //when & then
        mockMvc.perform(patch("/v1/trips/{tripKey}/finalize", TEST_TRIP_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("여행이 성공적으로 등록되었습니다."));
      }

      @Test
      @WithMockUser(username = TEST_EMAIL)
      @DisplayName("잘못된 상태의 여행에 요청하면 400 응답을 반환한다")
      void finalizeTrip_givenInvalidTripState_returns400() throws Exception {
        //given
        willThrow(new CustomException(ResultCode.INVALID_TRIP_STATE))
                .given(tripCommandService).finalizeTrip(any(User.class), eq(TEST_TRIP_KEY));

        //when & then
        mockMvc.perform(patch("/v1/trips/{tripKey}/finalize", TEST_TRIP_KEY))
                .andExpect(status().isBadRequest());
      }

      @Test
      @WithMockUser(username = TEST_EMAIL)
      @DisplayName("접근 권한이 없는 여행에 요청하면 403 응답을 반환한다")
      void finalizeTrip_givenUnauthorizedUser_returns403() throws Exception {
        //given
        willThrow(new CustomException(ResultCode.ACCESS_DENIED))
                .given(tripCommandService).finalizeTrip(any(User.class), eq(TEST_TRIP_KEY));

        //when & then
        mockMvc.perform(patch("/v1/trips/{tripKey}/finalize", TEST_TRIP_KEY))
                .andExpect(status().isForbidden());
      }
    }

    @Nested
    @DisplayName("PUT /v1/trips/{tripKey} — 여행 정보 등록/수정")
    class UpdateTrip {

      private TripUpdateRequest validRequest;

      @BeforeEach
      void setUp() {
        validRequest = new TripUpdateRequest(
                "도쿄 여행",
                "일본",
                LocalDate.of(2024, 5, 1),
                LocalDate.of(2024, 5, 7),
                List.of("여행", "맛집")
        );
      }

      @Test
      @WithMockUser(username = TEST_EMAIL)
      @DisplayName("유효한 요청으로 여행 정보를 수정하면 성공 메시지와 200을 반환한다")
      void updateTrip_givenValidRequest_returns200() throws Exception {
        //given
        willDoNothing().given(tripCommandService)
                .updateTrip(any(User.class), eq(TEST_TRIP_KEY), any(TripUpdateRequest.class));

        //when & then
        mockMvc.perform(put("/v1/trips/{tripKey}", TEST_TRIP_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("성공적으로 여행 정보가 등록되었습니다."));
      }

      @Test
      @WithMockUser(username = TEST_EMAIL)
      @DisplayName("존재하지 않는 tripKey로 요청하면 403 응답을 반환한다")
      void updateTrip_givenInvalidTripKey_returns403() throws Exception {
        //given
        willThrow(new CustomException(ResultCode.ACCESS_DENIED))
                .given(tripCommandService)
                .updateTrip(any(User.class), eq("INVALID_KEY"), any(TripUpdateRequest.class));

        //when & then
        mockMvc.perform(put("/v1/trips/{tripKey}", "INVALID_KEY")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden());
      }

      @Test
      @WithMockUser(username = TEST_EMAIL)
      @DisplayName("접근 권한이 없는 여행에 수정 요청하면 403 응답을 반환한다")
      void updateTrip_givenUnauthorizedUser_returns403() throws Exception {
        //given
        willThrow(new CustomException(ResultCode.ACCESS_DENIED))
                .given(tripCommandService)
                .updateTrip(any(User.class), eq(TEST_TRIP_KEY), any(TripUpdateRequest.class));

        //when & then
        mockMvc.perform(put("/v1/trips/{tripKey}", TEST_TRIP_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden());
      }
    }

    @Nested
    @DisplayName("DELETE /v1/trips/{tripKey} — 여행 정보 삭제")
    class DeleteTrip {

      @Test
      @WithMockUser(username = TEST_EMAIL)
      @DisplayName("소유자가 삭제 요청하면 성공 메시지와 200을 반환한다")
      void deleteTrip_givenOwner_returns200() throws Exception {
        //given
        willDoNothing().given(tripCommandService).deleteTrip(any(User.class), eq(TEST_TRIP_KEY));

        //when & then
        mockMvc.perform(delete("/v1/trips/{tripKey}", TEST_TRIP_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("성공적으로 여행 정보가 삭제되었습니다."));
      }

      @Test
      @WithMockUser(username = TEST_EMAIL)
      @DisplayName("소유자가 아닌 사용자가 삭제 요청하면 403 응답을 반환한다")
      void deleteTrip_givenNonOwner_returns403() throws Exception {
        //given
        willThrow(new CustomException(ResultCode.ACCESS_DENIED))
                .given(tripCommandService).deleteTrip(any(User.class), eq(TEST_TRIP_KEY));

        //when & then
        mockMvc.perform(delete("/v1/trips/{tripKey}", TEST_TRIP_KEY))
                .andExpect(status().isForbidden());
      }

      @Test
      @WithMockUser(username = TEST_EMAIL)
      @DisplayName("존재하지 않는 tripKey로 삭제 요청하면 403 응답을 반환한다")
      void deleteTrip_givenInvalidTripKey_returns403() throws Exception {
        //given
        willThrow(new CustomException(ResultCode.ACCESS_DENIED))
                .given(tripCommandService).deleteTrip(any(User.class), eq("INVALID_KEY"));

        //when & then
        mockMvc.perform(delete("/v1/trips/{tripKey}", "INVALID_KEY"))
                .andExpect(status().isForbidden());
      }


    }
  }
}
