package com.shortly.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Shortly URL shortener application.
 *
 * <p>Backend-only Spring Boot service backed by PostgreSQL (source of truth)
 * and Redis (cache-aside read path). Interactive API documentation is served
 * via springdoc OpenAPI; operational health via Spring Boot Actuator.
 * Scheduling backs the hourly expiration cleanup job.
 */
@EnableScheduling
@SpringBootApplication
public class ShortlyApplication {

    /**
     * Boots the Shortly application.
     *
     * @param args command line arguments passed to {@link SpringApplication}
     */
    public static void main(String[] args) {
        SpringApplication.run(ShortlyApplication.class, args);
    }
}
