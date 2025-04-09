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
                    .title("🗺️Trip Tyche API")
                    .version("1.0"))
            .addSecurityItem(new SecurityRequirement().addList("AccessTokenCookie"))
            .components(new Components()
                    .addSecuritySchemes("AccessTokenCookie",
                            new SecurityScheme()
                                    .type(SecurityScheme.Type.APIKEY)
                                    .in(SecurityScheme.In.COOKIE)
                                    .name("access_token"))
                    .addSecuritySchemes("RefreshTokenCookie",
                            new SecurityScheme()
                                    .type(SecurityScheme.Type.APIKEY)
                                    .in(SecurityScheme.In.COOKIE)
                                    .name("refresh_token"))
            )
            .tags(List.of(
                    new Tag().name("0. 로그인&인증관련 API"),
                    new Tag().name("1. 메인 페이지 API"),
                    new Tag().name("2. 여행관리 페이지 API"),
                    new Tag().name("3. 여행등록 페이지 API"),
                    new Tag().name("4. 이미지 수정 페이지 API"),
                    new Tag().name("5. Map 페이지 API"),
                    new Tag().name("6. 날짜별 페이지 API"),
                    new Tag().name("7. 위치정보 없는 수정 페이지 API"),
                    new Tag().name("8. 공유 관련 API"),
                    new Tag().name("9. 알림 관련 API")
            ));
  }
}
