package com.shortly.app.integration;

import com.shortly.app.dto.UrlResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cache-aside behaviour against a real Redis container.
 *
 * <p>Skipped automatically where Docker is unavailable.
 */
@EnabledIfDockerAvailable
class RedisCacheIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @LocalServerPort
    private int port;

    /**
     * Verifies creation warms the cache and a miss repopulates from the database.
     *
     * @throws Exception on HTTP client failures
     */
    @Test
    void cacheAside_populatesOnMiss() throws Exception {
        ResponseEntity<UrlResponse> created = restTemplate.postForEntity(
                "/api/urls",
                Map.of("originalUrl", "https://example.com/cache", "customAlias", "cache-probe-1"),
                UrlResponse.class);
        assertEquals(HttpStatus.CREATED, created.getStatusCode());
        assertNotNull(created.getBody());
        assertEquals("cache-probe-1", created.getBody().shortCode());

        String key = "url:cache-probe-1";
        assertTrue(Boolean.TRUE.equals(redisTemplate.hasKey(key)));

        redisTemplate.delete(key);
        assertFalse(Boolean.TRUE.equals(redisTemplate.hasKey(key)));

        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        HttpResponse<Void> redirect = client.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/cache-probe-1"))
                        .GET().build(),
                HttpResponse.BodyHandlers.discarding());
        assertEquals(302, redirect.statusCode());

        assertTrue(Boolean.TRUE.equals(redisTemplate.hasKey(key)));
        String json = redisTemplate.opsForValue().get(key);
        assertNotNull(json);
        assertTrue(json.contains("https://example.com/cache"));
    }
}
