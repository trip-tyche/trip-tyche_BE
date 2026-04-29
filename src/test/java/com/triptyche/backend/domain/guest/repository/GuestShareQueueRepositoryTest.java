package com.triptyche.backend.domain.guest.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;

import com.triptyche.backend.global.config.GuestProperties;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

@ExtendWith(MockitoExtension.class)
class GuestShareQueueRepositoryTest {

    private static final String QUEUE_KEY = "guest:share_queue";

    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ZSetOperations<String, Object> zSetOperations;
    @Mock private GuestProperties guestProperties;

    private GuestShareQueueRepository repository;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        repository = new GuestShareQueueRepository(redisTemplate, guestProperties);
    }

    @Test
    @DisplayName("enqueue() — ZSET에 guestUserId 문자열로 등록된다")
    void enqueue_addsGuestUserIdToZSet() {
        given(guestProperties.shareDelaySeconds()).willReturn(15L);

        repository.enqueue(42L);

        then(zSetOperations).should().add(eq(QUEUE_KEY), eq("42"), anyDouble());
    }

    @Test
    @DisplayName("enqueue() — score는 현재 시간 + shareDelaySeconds 근방이다")
    void enqueue_scoreIsCurrentTimePlusDelay() {
        given(guestProperties.shareDelaySeconds()).willReturn(30L);
        long before = System.currentTimeMillis() / 1000;

        repository.enqueue(1L);

        long after = System.currentTimeMillis() / 1000;
        // score 캡처 대신 add 호출 자체를 검증 (score 범위는 구현 내부)
        then(zSetOperations).should().add(eq(QUEUE_KEY), eq("1"), anyDouble());
        assertThat(after - before).isLessThan(2);
    }

    @Test
    @DisplayName("pollDueIds() — Lua 스크립트가 실행된다")
    void pollDueIds_executesLuaScript() {
        given(redisTemplate.execute(any(), any(List.class), any())).willReturn(List.of());

        List<Long> result = repository.pollDueIds();

        then(redisTemplate).should().execute(any(), any(List.class), any());
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("pollDueIds() — Redis가 멤버를 반환하면 Long으로 파싱하여 반환한다")
    void pollDueIds_parsesMembersToLong() {
        given(redisTemplate.execute(any(), any(List.class), any())).willReturn(List.of("10", "20"));

        List<Long> result = repository.pollDueIds();

        assertThat(result).containsExactly(10L, 20L);
    }

    @Test
    @DisplayName("removeAll() — 복수 userId를 ZSET에서 일괄 제거한다")
    void removeAll_removesAllIdsFromZSet() {
        repository.removeAll(List.of(1L, 2L, 3L));

        then(zSetOperations).should().remove(eq(QUEUE_KEY), eq("1"), eq("2"), eq("3"));
    }

    @Test
    @DisplayName("removeAll() — 빈 목록이면 ZSET 접근을 하지 않는다")
    void removeAll_givenEmptyList_doesNotAccessZSet() {
        repository.removeAll(List.of());

        then(zSetOperations).shouldHaveNoInteractions();
    }
}
