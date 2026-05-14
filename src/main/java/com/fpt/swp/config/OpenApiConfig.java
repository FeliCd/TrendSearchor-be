package com.fpt.swp.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfig {

        @Bean
        public OpenAPI customOpenAPI() {
                return new OpenAPI()
                                .addServersItem(new Server().url("https://trendsearchor-be-production.up.railway.app").description("Production Server"))
                                .addServersItem(new Server().url("http://localhost:8080").description("Local Server"))
                                .info(new Info()
                                                .title("Journal Trend Tracker API")
                                                .version("1.0")
                                                .description("Hệ thống theo dõi xu hướng báo chí khoa học"));
        }
}
