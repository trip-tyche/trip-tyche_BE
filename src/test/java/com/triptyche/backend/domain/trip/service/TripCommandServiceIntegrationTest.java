package com.triptyche.backend.domain.trip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.triptyche.backend.domain.media.service.UnlocatedMediaCacheService;
import com.triptyche.backend.domain.trip.model.Trip;
import com.triptyche.backend.domain.trip.model.TripStatus;
import com.triptyche.backend.domain.trip.repository.TripRepository;
import com.triptyche.backend.domain.user.model.User;
import com.triptyche.backend.domain.user.repository.UserRepository;
import com.triptyche.backend.global.redis.UnlocatedMediaHashRepository;
import com.triptyche.backend.global.s3.S3UploadService;
import com.triptyche.backend.global.util.JwtTokenProvider;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("local")
class TripCommandServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private EntityManager entityManager;

    @MockBean
    private UnlocatedMediaHashRepository imageQueueService;

    @MockBean
    private S3UploadService s3UploadService;

    @MockBean
    private UnlocatedMediaCacheService unlocatedMediaCacheService;

    @MockBean
    private RedisTemplate<String, String> redisTemplate;

    @MockBean
    private SimpMessagingTemplate simpMessagingTemplate;

    private User owner;
    private User otherUser;
    private Trip trip;
    private String ownerToken;
    private String otherUserToken;

    @BeforeEach
    void setUp() {
        owner = userRepository.save(User.builder()
                .userName("소유자")
                .userNickName("owner-nick")
                .userEmail("owner@test.com")
                .provider("kakao")
                .build());

        otherUser = userRepository.save(User.builder()
                .userName("타인")
                .userNickName("other-nick")
                .userEmail("other@test.com")
                .provider("kakao")
                .build());

        trip = tripRepository.save(Trip.builder()
                .user(owner)
                .tripTitle("테스트 여행")
                .country("일본")
                .startDate(LocalDate.of(2026, 3, 1))
                .endDate(LocalDate.of(2026, 3, 5))
                .hashtags("도쿄,벚꽃")
                .status(TripStatus.CONFIRMED)
                .build());

        ownerToken = jwtTokenProvider.createAccessToken(
                owner.getUserEmail(), List.of("ROLE_USER"), "kakao");
        otherUserToken = jwtTokenProvider.createAccessToken(
                otherUser.getUserEmail(), List.of("ROLE_USER"), "kakao");
    }

    @Test
    @DisplayName("소유자가 여행을 삭제하면 soft delete 처리된다")
    void deleteTrip_asOwner_softDeletes() throws Exception {
        String tripKey = trip.getTripKey();

        mockMvc.perform(delete("/v1/trips/{tripKey}", tripKey)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        entityManager.flush();
        entityManager.clear();

        assertThat(tripRepository.findById(trip.getTripId())).isEmpty();
    }

    @Test
    @DisplayName("비소유자가 여행을 삭제하면 403 응답을 반환한다")
    void deleteTrip_asNonOwner_returnsForbidden() throws Exception {
        String tripKey = trip.getTripKey();

        mockMvc.perform(delete("/v1/trips/{tripKey}", tripKey)
                        .header("Authorization", "Bearer " + otherUserToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(2009));

        assertThat(tripRepository.findById(trip.getTripId())).isPresent();
    }

    @Test
    @DisplayName("토큰 없이 여행을 삭제하면 401 응답을 반환한다")
    void deleteTrip_withoutToken_returnsUnauthorized() throws Exception {
        String tripKey = trip.getTripKey();

        mockMvc.perform(delete("/v1/trips/{tripKey}", tripKey))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1001));

        assertThat(tripRepository.findById(trip.getTripId())).isPresent();
    }

    @Test
    @DisplayName("잘못된 토큰으로 여행을 삭제하면 401 응답을 반환한다")
    void deleteTrip_withInvalidToken_returnsUnauthorized() throws Exception {
        String tripKey = trip.getTripKey();

        // provider가 포함된 유효한 JWT 구조이지만 서명이 틀린 토큰
        String forgedToken = ownerToken.substring(0, ownerToken.lastIndexOf('.')) + ".AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

        mockMvc.perform(delete("/v1/trips/{tripKey}", tripKey)
                        .header("Authorization", "Bearer " + forgedToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(2002));

        assertThat(tripRepository.findById(trip.getTripId())).isPresent();
    }
}
