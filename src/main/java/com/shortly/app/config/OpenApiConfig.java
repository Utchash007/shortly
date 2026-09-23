package com.shortly.app.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI metadata for the interactive Swagger UI.
 *
 * <p>Endpoint grouping comes from the {@code @Tag} annotations on the
 * controllers; this class supplies the API title, version and description.
 */
@Configuration
public class OpenApiConfig {

    /**
     * Creates the OpenAPI definition served at {@code /v3/api-docs}.
     *
     * @return the API metadata
     */
    @Bean
    public OpenAPI shortlyOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Shortly URL Shortener API")
                .version("1.0.0")
                .description("High-performance URL shortener using Spring Boot, PostgreSQL, Redis and analytics."));
    }
}
