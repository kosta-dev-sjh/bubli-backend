package com.bubli.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Bubli API")
                        .version("v1")
                        .description("Bubli 백엔드 API 문서"))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .name(BEARER_AUTH)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("로그인 후 발급된 Access Token을 입력하세요. (Bearer 접두사 불필요)")));
    }

    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
                .group("auth")
                .displayName("인증")
                .pathsToMatch("/api/auth/**")
                .build();
    }

    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("user")
                .displayName("사용자")
                .pathsToMatch("/api/me/**", "/api/users/**")
                .build();
    }

    @Bean
    public GroupedOpenApi projectApi() {
        return GroupedOpenApi.builder()
                .group("project")
                .displayName("프로젝트룸")
                .pathsToMatch("/api/project-rooms/**")
                .build();
    }

    @Bean
    public GroupedOpenApi resourceApi() {
        return GroupedOpenApi.builder()
                .group("resource")
                .displayName("자료")
                .pathsToMatch("/api/resources/**", "/api/resource-comments/**")
                .build();
    }

    @Bean
    public GroupedOpenApi workApi() {
        return GroupedOpenApi.builder()
                .group("work")
                .displayName("업무 (Task·WBS·일정)")
                .pathsToMatch("/api/tasks/**", "/api/wbs-items/**", "/api/schedules/**")
                .build();
    }

    @Bean
    public GroupedOpenApi chatApi() {
        return GroupedOpenApi.builder()
                .group("chat")
                .displayName("채팅")
                .pathsToMatch("/api/chat-rooms/**", "/api/chat/**")
                .build();
    }

    @Bean
    public GroupedOpenApi agentApi() {
        return GroupedOpenApi.builder()
                .group("agent")
                .displayName("AI 에이전트")
                .pathsToMatch("/api/agent-jobs/**", "/api/agent-suggestions/**", "/api/ai-documents/**")
                .build();
    }

    @Bean
    public GroupedOpenApi notificationApi() {
        return GroupedOpenApi.builder()
                .group("notification")
                .displayName("알림")
                .pathsToMatch("/api/notifications/**")
                .build();
    }

    @Bean
    public GroupedOpenApi timerApi() {
        return GroupedOpenApi.builder()
                .group("timer")
                .displayName("타이머")
                .pathsToMatch("/api/time-logs/**")
                .build();
    }

    @Bean
    public GroupedOpenApi dashboardApi() {
        return GroupedOpenApi.builder()
                .group("dashboard")
                .displayName("대시보드")
                .pathsToMatch("/api/dashboard/**")
                .build();
    }

    @Bean
    public GroupedOpenApi storageApi() {
        return GroupedOpenApi.builder()
                .group("storage")
                .displayName("스토리지")
                .pathsToMatch("/api/storage/**")
                .build();
    }
}
