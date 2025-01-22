package com.fivefeeling.memory.global.config;

import com.fivefeeling.memory.global.redis.RedisStreamMessageListener;
import java.time.Duration;
import java.util.concurrent.Executors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions;

@Configuration
public class RedisConfig {

  @Value("${spring.data.redis.host}")
  private String host;

  @Bean
  public RedisConnectionFactory redisConnectionFactory() {
    // Lettuce를 Redis 연결팩토리로 사용
    LettuceConnectionFactory factory = new LettuceConnectionFactory(host, 6379);
    factory.afterPropertiesSet();
    return factory;
  }

  @Bean
  public RedisTemplate<String, Object> redisTemplate() {
    RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
    redisTemplate.setConnectionFactory(redisConnectionFactory());
    // Key 직렬화
    redisTemplate.setKeySerializer(new StringRedisSerializer());
    redisTemplate.setHashKeySerializer(new StringRedisSerializer());
    // Value 직렬화: JSON 형식
    redisTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));
    redisTemplate.setHashValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));
    redisTemplate.afterPropertiesSet();
    return redisTemplate;
  }

  @Bean
  public StreamMessageListenerContainer<String, MapRecord<String, String, String>> redisStreamListenerContainer(
          RedisConnectionFactory connectionFactory,
          RedisStreamMessageListener listener) {

    StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
            StreamMessageListenerContainerOptions.builder()
                    .batchSize(10) // 한 번에 읽어올 메시지 수
                    .pollTimeout(Duration.ofSeconds(1)) // 폴링 타임아웃
                    .executor(Executors.newSingleThreadExecutor()) // 스레드 풀 설정
                    .build();

    StreamMessageListenerContainer<String, MapRecord<String, String, String>> container =
            StreamMessageListenerContainer.create(connectionFactory, options);

    container.receive(StreamOffset.fromStart("shareRequests"), listener);
    container.start();

    return container;
  }

}
