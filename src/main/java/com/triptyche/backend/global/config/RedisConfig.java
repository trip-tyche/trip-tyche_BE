package com.triptyche.backend.global.config;

import com.triptyche.backend.global.redis.MediaProcessedStreamConsumer;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import java.net.InetAddress;
import java.time.Duration;
import java.util.Map;
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
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

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

  @Bean
  public StreamMessageListenerContainer<String, MapRecord<String, String, String>> mediaProcessedStreamContainer(
      RedisConnectionFactory connectionFactory,
      MediaProcessedStreamConsumer consumer
  ) {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(4);
    executor.setQueueCapacity(16);
    executor.setThreadNamePrefix("media-processed-");
    executor.initialize();

    StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
        StreamMessageListenerContainer.StreamMessageListenerContainerOptions.builder()
            .pollTimeout(Duration.ofSeconds(2))
            .serializer(new StringRedisSerializer())
            .executor(executor)
            .build();

    StreamMessageListenerContainer<String, MapRecord<String, String, String>> container =
        StreamMessageListenerContainer.create(connectionFactory, options);

    // 스트림 + 컨슈머 그룹 생성 (이미 존재하면 무시)
    try {
      redisTemplate().opsForStream().createGroup("media-processed-stream", "media-processed-workers");
    } catch (Exception busyGroup) {
      // BUSYGROUP: 이미 존재 — 무시
      // NOGROUP / stream 없음 에러면 XADD로 stream을 먼저 생성 후 재시도
      try {
        redisTemplate().opsForStream().add(
            MapRecord.create("media-processed-stream", Map.of("init", "true")));
        redisTemplate().opsForStream().createGroup("media-processed-stream", "media-processed-workers");
      } catch (Exception e) {
        // 재시도 실패 시에도 컨테이너는 기동 — 스트림이 추후 생성되면 자동 구독
      }
    }

    String consumerName;
    try {
      consumerName = InetAddress.getLocalHost().getHostName();
    } catch (Exception e) {
      consumerName = "be-1";
    }

    container.receive(
        Consumer.from("media-processed-workers", consumerName),
        StreamOffset.create("media-processed-stream", ReadOffset.lastConsumed()),
        consumer
    );

    container.start();
    return container;
  }
}
