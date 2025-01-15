package com.fivefeeling.memory.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

  @Bean
  public OpenAPI customOpenAPI() {
    return new OpenAPI()
            .info(new Info()
                    .title("Trip Tyche API")
                    .version("1.0"))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
            .components(new Components()
                    .addSecuritySchemes("bearerAuth",
                            new SecurityScheme()
                                    .name("bearerAuth")
                                    .type(SecurityScheme.Type.HTTP)
                                    .scheme("bearer")
                                    .bearerFormat("JWT")))
            .tags(List.of(
                    new Tag().name("1. 메인 페이지 API"),
                    new Tag().name("2. 여행관리 페이지 API"),
                    new Tag().name("3. 여행등록 페이지 API"),
                    new Tag().name("4. Map 페이지 API"),
                    new Tag().name("5. 날짜별 페이지 API"),
                    new Tag().name("6. 위치정보 없는 수정 페이지 API"),
                    new Tag().name("7. 공유 관련 API"),
                    new Tag().name("사용하지 않는 API")));
  }
}