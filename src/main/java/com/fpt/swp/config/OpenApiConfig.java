package com.fpt.swp.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

    @Value("${app.base-url:https://trendsearchor-be-production.up.railway.app}")
    private String baseUrl;

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        OpenAPI openApi = new OpenAPI()
                .info(new Info()
                        .title("TrendSearchor API")
                        .version("1.0")
                        .description("Hệ thống theo dõi xu hướng báo chí khoa học"))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                // Local dev server first — users can switch to production in the Swagger dropdown
                .addServersItem(new Server().url("http://localhost:8080").description("Local Development"))
                .addServersItem(new Server().url(baseUrl).description("Production Server"));

        return openApi;
    }
}
