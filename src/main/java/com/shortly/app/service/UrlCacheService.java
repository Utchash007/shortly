package com.shortly.app.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Cache-aside operations for short URL resolution.
 *
 * <p>Keys look like {@code url:{codeOrAlias}} and hold JSON
 * {@code {originalUrl, expiresAt, active}} so expiration and deactivation can
 * be enforced without touching PostgreSQL. Redis is a performance layer only:
 * every failure degrades to a miss and the caller falls back to the database.
 */
@Service
public class UrlCacheService {

    private static final Logger logger = LoggerFactory.getLogger(UrlCacheService.class);

    /**
     * Key prefix for cached URL entries.
     */
    static final String KEY_PREFIX = "url:";

    /**
     * Cached form of a URL record.
     *
     * <p>{@code urlId} links cache hits back to the database row for click
     * recording. It is nullable so entries written before Phase 6 still
     * deserialize; a null id falls back to a database lookup.
     *
     * @param originalUrl destination URL
     * @param expiresAt expiration timestamp, null for never-expiring links
     * @param active whether the link currently redirects
     * @param urlId database identifier, may be null for legacy entries
     */
    public record CachedUrl(String originalUrl, Instant expiresAt, boolean active, Long urlId) {
    }

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration defaultTtl;

    /**
     * Creates the service with its required collaborators.
     *
     * @param redisTemplate string template from {@code RedisConfig}
     * @param objectMapper Boot-configured Jackson 3 mapper with Java-time support
     * @param defaultTtl time-to-live for links without expiration, from {@code app.cache.default-ttl}
     */
    public UrlCacheService(StringRedisTemplate redisTemplate,
                           ObjectMapper objectMapper,
                           @Value("${app.cache.default-ttl:PT1H}") Duration defaultTtl) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.defaultTtl = defaultTtl;
    }

    /**
     * Reads a cached entry.
     *
     * @param codeOrAlias the short code or alias
     * @return the cached value, or empty on miss, corrupt entry or Redis failure
     */
    public Optional<CachedUrl> get(String codeOrAlias) {
        String key = key(codeOrAlias);
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                logger.debug("Cache miss for {}", key);
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, CachedUrl.class));
        } catch (JacksonException e) {
            logger.warn("Corrupt cache entry for {}, evicting", key);
            evict(codeOrAlias);
            return Optional.empty();
        } catch (DataAccessException e) {
            logger.warn("Redis unavailable during GET {}, falling back to database", key);
            return Optional.empty();
        }
    }

    /**
     * Stores an entry with an expiration-aware time-to-live.
     *
     * <p>Expiring links live exactly until {@code expiresAt}; all others use
     * the configured default. Already-expired values are never cached.
     * Failures are logged and swallowed so writes never break requests.
     *
     * @param codeOrAlias the short code or alias
     * @param value the value to cache
     */
    public void put(String codeOrAlias, CachedUrl value) {
        Duration ttl = value.expiresAt() != null
                ? Duration.between(Instant.now(), value.expiresAt())
                : defaultTtl;
        if (ttl.isZero() || ttl.isNegative()) {
            logger.debug("Skipping cache write for expired {}", codeOrAlias);
            return;
        }
        try {
            redisTemplate.opsForValue().set(key(codeOrAlias), objectMapper.writeValueAsString(value), ttl);
        } catch (JacksonException | DataAccessException e) {
            logger.warn("Redis unavailable during SET {}, continuing without cache", codeOrAlias);
        }
    }

    /**
     * Removes entries for the given codes or aliases.
     *
     * <p>Failures are logged and swallowed.
     *
     * @param codesOrAliases short codes and/or aliases to evict
     */
    public void evict(String... codesOrAliases) {
        List<String> keys = Arrays.stream(codesOrAliases).map(this::key).toList();
        if (keys.isEmpty()) {
            return;
        }
        try {
            redisTemplate.delete(keys);
        } catch (DataAccessException e) {
            logger.warn("Redis unavailable during DEL {}, continuing without cache", keys);
        }
    }

    /**
     * Builds the Redis key for a short code or alias.
     *
     * @param codeOrAlias the short code or alias
     * @return the namespaced key
     */
    private String key(String codeOrAlias) {
        return KEY_PREFIX + codeOrAlias;
    }
}
