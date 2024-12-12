package com.fivefeeling.memory.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

  @Bean
  public RedisConnectionFactory redisConnectionFactory() {
    // Lettuce를 Redis 연결팩토리로 사용
    LettuceConnectionFactory factory = new LettuceConnectionFactory("redis", 6379);
    factory.afterPropertiesSet();
    return factory;
  }

  @Bean
  public RedisTemplate<String, Object> redisTemplate(ObjectMapper objectMapper) {
    RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
    redisTemplate.setConnectionFactory(redisConnectionFactory());

    // Key 직렬화: 사람이 읽을 수 있는 문자열
    redisTemplate.setKeySerializer(new StringRedisSerializer());
    redisTemplate.setHashKeySerializer(new StringRedisSerializer());

    // Value 직렬화: JSON 형식
    redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer(objectMapper));
    redisTemplate.setHashValueSerializer(new GenericJackson2JsonRedisSerializer(objectMapper));

    return redisTemplate;
  }
}
