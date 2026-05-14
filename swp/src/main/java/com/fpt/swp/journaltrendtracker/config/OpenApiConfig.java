package com.fpt.swp.journaltrendtracker.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Journal Trend Tracker API")
                        .version("1.0")
                        .description("Hệ thống theo dõi xu hướng báo chí khoa học"));
    }
}
