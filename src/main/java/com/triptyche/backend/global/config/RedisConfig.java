package com.triptyche.backend.global.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import java.time.Duration;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.data.redis.RedisHealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

  @Value("${spring.data.redis.host}")
  private String host;

  @Value("${spring.data.redis.port:6379}")
  private int port;

  @Value("${spring.data.redis.timeout:5000}")
  private int timeout;

  @Value("${spring.data.redis.password:}")
  private String password;

  @Bean
  public RedisConnectionFactory redisConnectionFactory() {

    // Redis 서버 구성
    RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration(host, port);

    // 비밀번호 설정 추가
    if (password != null && !password.isEmpty()) {
      redisConfig.setPassword(password);
    }

    // 커넥션 풀 설정 - 제네릭 타입 지정 (Object)
    GenericObjectPoolConfig<Object> poolConfig = new GenericObjectPoolConfig<>();
    poolConfig.setMaxTotal(8);         // 최대 연결 수
    poolConfig.setMaxIdle(8);          // 최대 유휴 연결 수
    poolConfig.setMinIdle(2);          // 최소 유휴 연결 수 (중요: 연결 유지를 위한 설정)
    poolConfig.setTestOnBorrow(true);  // 연결 가져올 때 테스트
    poolConfig.setTestOnReturn(true);  // 연결 반환 시 테스트
    poolConfig.setTestWhileIdle(true); // 유휴 상태 테스트

    // Lettuce 클라이언트 옵션 설정
    SocketOptions socketOptions = SocketOptions.builder()
            .connectTimeout(Duration.ofMillis(timeout))
            .build();

    ClientOptions clientOptions = ClientOptions.builder()
            .socketOptions(socketOptions)
            .autoReconnect(true)
            .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
            .build();

    // Lettuce 클라이언트 구성
    LettuceClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
            .poolConfig(poolConfig)
            .clientOptions(clientOptions)
            .commandTimeout(Duration.ofMillis(timeout))
            .shutdownTimeout(Duration.ofSeconds(5))
            .build();

    // Lettuce 연결 팩토리 생성
    LettuceConnectionFactory factory = new LettuceConnectionFactory(redisConfig, clientConfig);
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

    // Value 직렬화
    redisTemplate.setValueSerializer(new StringRedisSerializer());
    redisTemplate.setHashValueSerializer(new StringRedisSerializer());

    redisTemplate.setEnableDefaultSerializer(false);
    redisTemplate.afterPropertiesSet();
    return redisTemplate;
  }
  // Redis 연결 상태 확인을 위한 헬스 체크 빈
  @Bean
  public RedisHealthIndicator redisHealthIndicator(RedisConnectionFactory redisConnectionFactory) {
    return new RedisHealthIndicator(redisConnectionFactory);
  }
}
