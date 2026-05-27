package com.triptyche.backend.global.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.triptyche.backend.domain.media.event.MediaFileRegisteredEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;

@ExtendWith(MockitoExtension.class)
class ImageProcessingPublisherTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private StreamOperations<String, Object, Object> streamOperations;

    @InjectMocks
    private ImageProcessingPublisher imageProcessingPublisher;

    @BeforeEach
    void setUp() {
        given(redisTemplate.opsForStream()).willReturn(streamOperations);
    }

    @Nested
    @DisplayName("v1 이벤트")
    class V1Event {

        @Test
        @DisplayName("payload에 mediaFileId + originalKey만 포함, flow 키 없음")
        void v1Event_payloadContainsMediaFileIdAndOriginalKeyOnly() {
            // given
            MediaFileRegisteredEvent event = MediaFileRegisteredEvent.v1(42L, "originals/TRIP-KEY/photo.jpg");

            // when
            imageProcessingPublisher.publish(event);

            // then
            ArgumentCaptor<MapRecord<String, Object, Object>> captor =
                    ArgumentCaptor.forClass(MapRecord.class);
            verify(streamOperations).add(captor.capture());

            MapRecord<String, Object, Object> record = captor.getValue();
            assertThat(record.getValue()).containsKey("mediaFileId");
            assertThat(record.getValue()).containsKey("originalKey");
            assertThat(record.getValue()).doesNotContainKey("flow");
            assertThat(record.getValue()).doesNotContainKey("tempKey");
            assertThat(record.getValue()).doesNotContainKey("finalKey");
            assertThat(record.getValue().get("mediaFileId")).isEqualTo("42");
            assertThat(record.getValue().get("originalKey")).isEqualTo("originals/TRIP-KEY/photo.jpg");
        }
    }

    @Nested
    @DisplayName("v2 이벤트")
    class V2Event {

        @Test
        @DisplayName("payload에 mediaFileId + tempKey + finalKey + flow=v2 포함")
        void v2Event_payloadContainsAllV2Fields() {
            // given
            MediaFileRegisteredEvent event = MediaFileRegisteredEvent.v2(
                    77L, "temp/42/uuid1.jpg", "processed/42/uuid1.webp");

            // when
            imageProcessingPublisher.publish(event);

            // then
            ArgumentCaptor<MapRecord<String, Object, Object>> captor =
                    ArgumentCaptor.forClass(MapRecord.class);
            verify(streamOperations).add(captor.capture());

            MapRecord<String, Object, Object> record = captor.getValue();
            assertThat(record.getValue()).containsKey("mediaFileId");
            assertThat(record.getValue()).containsKey("tempKey");
            assertThat(record.getValue()).containsKey("finalKey");
            assertThat(record.getValue()).containsKey("flow");
            assertThat(record.getValue()).doesNotContainKey("originalKey");
            assertThat(record.getValue().get("mediaFileId")).isEqualTo("77");
            assertThat(record.getValue().get("tempKey")).isEqualTo("temp/42/uuid1.jpg");
            assertThat(record.getValue().get("finalKey")).isEqualTo("processed/42/uuid1.webp");
            assertThat(record.getValue().get("flow")).isEqualTo("v2");
        }
    }
}
