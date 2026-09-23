package com.shortly.app;

import com.shortly.app.util.DotenvLoader;
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
     * <p>A {@code .env} file is applied first so secret mounts work without
     * dashboard variables; real environment entries always take precedence.
     *
     * @param args command line arguments passed to {@link SpringApplication}
     */
    public static void main(String[] args) {
        DotenvLoader.load();
        SpringApplication.run(ShortlyApplication.class, args);
    }
}
