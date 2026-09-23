package com.shortly.app.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;

/**
 * Best-effort IP-to-country resolution with a Redis-backed cache.
 *
 * <p>Results are cached for 30 days as {@code ip:{address} -> country} so
 * repeat visitors never trigger repeat external calls. Any failure (cache or
 * API) yields {@code UNKNOWN} and must never break the redirect path.
 */
@Service
public class GeolocationService {

    private static final Logger logger = LoggerFactory.getLogger(GeolocationService.class);

    /**
     * Fallback country when geolocation is unavailable.
     */
    public static final String UNKNOWN = "UNKNOWN";

    /**
     * Key prefix for cached IP lookups.
     */
    static final String CACHE_PREFIX = "ip:";

    /**
     * How long IP lookups stay cached.
     */
    static final Duration CACHE_TTL = Duration.ofDays(30);

    private static final Duration API_TIMEOUT = Duration.ofSeconds(2);

    private final StringRedisTemplate redisTemplate;
    private final RestClient restClient;

    /**
     * Creates the service with its required collaborators.
     *
     * @param redisTemplate string template from {@code RedisConfig}
     */
    public GeolocationService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(API_TIMEOUT);
        requestFactory.setReadTimeout(API_TIMEOUT);
        this.restClient = RestClient.builder().baseUrl("http://ip-api.com").requestFactory(requestFactory).build();
    }

    /**
     * Resolves an IP address to a country code.
     *
     * @param ipAddress the client IP, may be null
     * @return the country code, or {@code UNKNOWN} on any failure
     */
    public String lookupCountry(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return UNKNOWN;
        }
        String key = CACHE_PREFIX + ipAddress;
        try {
            String cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                return cached;
            }
        } catch (DataAccessException e) {
            logger.debug("Geo cache unreadable for {}, querying API directly", ipAddress);
        }
        String country = queryApi(ipAddress);
        try {
            redisTemplate.opsForValue().set(key, country, CACHE_TTL);
        } catch (DataAccessException e) {
            logger.debug("Geo cache unwritable for {}", ipAddress);
        }
        return country;
    }

    /**
     * Queries the external geolocation API.
     *
     * @param ipAddress the client IP
     * @return the country code, or {@code UNKNOWN} when the lookup fails
     */
    @SuppressWarnings("unchecked")
    private String queryApi(String ipAddress) {
        try {
            Map<String, Object> body = restClient.get()
                    .uri("/json/{ip}?fields=status,countryCode", ipAddress)
                    .retrieve()
                    .body(Map.class);
            if (body != null
                    && "success".equals(body.get("status"))
                    && body.get("countryCode") instanceof String code
                    && !code.isBlank()) {
                return code;
            }
        } catch (Exception e) {
            logger.debug("Geolocation API failed for {}: {}", ipAddress, e.toString());
        }
        return UNKNOWN;
    }
}
