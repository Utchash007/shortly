package com.shortly.app.integration;

import com.shortly.app.dto.AnalyticsResponse;
import com.shortly.app.dto.UrlResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end flow against real PostgreSQL and Redis containers.
 *
 * <p>Skipped automatically where Docker is unavailable.
 */
@EnabledIfDockerAvailable
class UrlShortenerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    /**
     * Exercises create, redirect, analytics, QR and delete in one flow.
     *
     * @throws Exception on HTTP client failures
     */
    @Test
    void fullFlow_createRedirectAnalyticsQrDelete() throws Exception {
        ResponseEntity<UrlResponse> created = restTemplate.postForEntity(
                "/api/urls", Map.of("originalUrl", "https://example.com/integration"), UrlResponse.class);
        assertEquals(HttpStatus.CREATED, created.getStatusCode());
        UrlResponse body = created.getBody();
        assertNotNull(body);

        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        HttpResponse<Void> redirect = client.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/" + body.shortCode()))
                        .GET().build(),
                HttpResponse.BodyHandlers.discarding());
        assertEquals(302, redirect.statusCode());
        assertEquals("https://example.com/integration",
                redirect.headers().firstValue("location").orElseThrow());

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            AnalyticsResponse analytics = restTemplate.getForObject(
                    "/api/urls/" + body.id() + "/analytics", AnalyticsResponse.class);
            assertNotNull(analytics);
            assertEquals(1, analytics.totalClicks());
        });

        ResponseEntity<byte[]> qr = restTemplate.getForEntity(
                "/api/urls/" + body.id() + "/qr", byte[].class);
        assertEquals(HttpStatus.OK, qr.getStatusCode());
        assertEquals(MediaType.IMAGE_PNG, qr.getHeaders().getContentType());
        assertNotNull(qr.getBody());
        assertTrue(qr.getBody().length > 0);

        ResponseEntity<Void> deleted = restTemplate.exchange(
                "/api/urls/" + body.id(), org.springframework.http.HttpMethod.DELETE, null, Void.class);
        assertEquals(HttpStatus.NO_CONTENT, deleted.getStatusCode());

        HttpResponse<Void> gone = client.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/" + body.shortCode()))
                        .GET().build(),
                HttpResponse.BodyHandlers.discarding());
        assertEquals(410, gone.statusCode());
    }
}
