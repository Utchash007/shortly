package com.shortly.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Background executors keeping the redirect path fast.
 *
 * <p>Click recording and geolocation run off-request on the analytics pool;
 * bursts of traffic queue instead of blocking redirect threads.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Creates the executor for asynchronous analytics work.
     *
     * @return bounded pool with an analytics thread prefix
     */
    @Bean(name = "analyticsExecutor")
    public Executor analyticsExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("analytics-");
        executor.initialize();
        return executor;
    }
}
