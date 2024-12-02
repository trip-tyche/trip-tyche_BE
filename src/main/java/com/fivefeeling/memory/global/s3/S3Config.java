package com.fivefeeling.memory.global.s3;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class S3Config {

  @Value("${spring.cloud.aws.credentials.accessKey}")
//  @Value("${AWS_ACCESS_KEY}")
  private String accessKey;

  @Value("${spring.cloud.aws.credentials.secretKey}")
//  @Value("${AWS_SECRET_KEY}")
  private String secretKey;

  @Value("${spring.cloud.aws.s3.bucketName}")
  private String bucketName;

  @Value("${spring.cloud.aws.region.static}")
  private String region;

  @Bean
  public S3Client s3Client() {
    AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(accessKey, secretKey);

    return S3Client.builder()
        .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
        .region(Region.of(region))
        .build();
  }

  @Bean
  public String bucketName() {
    return bucketName;
  }
}
