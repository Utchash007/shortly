package com.shortly.app.service;

import com.shortly.app.dto.CreateUrlRequest;
import com.shortly.app.dto.UrlResponse;
import com.shortly.app.entity.Url;
import com.shortly.app.exception.AliasAlreadyExistsException;
import com.shortly.app.exception.UrlExpiredException;
import com.shortly.app.exception.UrlNotFoundException;
import com.shortly.app.repository.UrlRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Core URL shortening logic: creation, resolution, lookup and soft deletion.
 *
 * <p>Stateless service with declarative transaction boundaries. Short codes and
 * custom aliases share one redirect namespace: a supplied alias becomes the
 * lookup key itself. The database {@code UNIQUE} constraint is the final guard
 * against concurrent-insert races.
 *
 * <p>Resolution follows the cache-aside pattern through {@link UrlCacheService}:
 * Redis first, PostgreSQL fallback, repopulate on miss. Redis outages never
 * fail requests.
 */
@Service
public class UrlService {

    private static final Logger logger = LoggerFactory.getLogger(UrlService.class);

    /**
     * Collision retries before giving up on random code generation.
     */
    static final int MAX_GENERATION_ATTEMPTS = 5;

    private final UrlRepository urlRepository;
    private final ShortCodeGenerator shortCodeGenerator;
    private final UrlCacheService urlCacheService;
    private final String baseUrl;

    /**
     * Creates the service with its required collaborators.
     *
     * @param urlRepository persistence for URL records
     * @param shortCodeGenerator random code generator
     * @param urlCacheService cache-aside read path
     * @param baseUrl public application host used to build {@code shortUrl}, from {@code app.base-url}
     */
    public UrlService(UrlRepository urlRepository,
                      ShortCodeGenerator shortCodeGenerator,
                      UrlCacheService urlCacheService,
                      @Value("${app.base-url:http://localhost:8080}") String baseUrl) {
        this.urlRepository = urlRepository;
        this.shortCodeGenerator = shortCodeGenerator;
        this.urlCacheService = urlCacheService;
        this.baseUrl = baseUrl;
    }

    /**
     * Creates a shortened URL from the request.
     *
     * @param request validated creation payload
     * @return the public view of the created link
     * @throws IllegalArgumentException when {@code expiresAt} lies in the past
     * @throws AliasAlreadyExistsException when the alias or code is already taken
     */
    @Transactional
    public UrlResponse createUrl(CreateUrlRequest request) {
        if (request.expiresAt() != null && request.expiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("expiresAt must be in the future");
        }
        String effectiveCode = request.customAlias() != null
                ? claimAlias(request.customAlias())
                : generateUniqueCode();
        try {
            Url saved = urlRepository.save(
                    Url.create(request.originalUrl(), effectiveCode, request.customAlias(), request.expiresAt()));
            urlCacheService.put(saved.getShortCode(), cached(saved));
            logger.info("Created short URL {} for {}", saved.getShortCode(), saved.getOriginalUrl());
            return toResponse(saved);
        } catch (DataIntegrityViolationException e) {
            logger.info("Short code collision on insert for {}", effectiveCode);
            throw new AliasAlreadyExistsException("Short code already exists: " + effectiveCode);
        }
    }

    /**
     * Resolves a short code or custom alias to its destination URL.
     *
     * <p>Cache-aside: Redis first, PostgreSQL fallback with repopulation.
     *
     * @param codeOrAlias the path value from {@code GET /{codeOrAlias}}
     * @return the original URL to redirect to
     * @throws UrlNotFoundException when nothing matches the supplied value
     * @throws UrlExpiredException when the link is deactivated or past expiry
     */
    @Transactional(readOnly = true)
    public String resolveUrl(String codeOrAlias) {
        Optional<UrlCacheService.CachedUrl> cached = urlCacheService.get(codeOrAlias);
        if (cached.isPresent()) {
            UrlCacheService.CachedUrl entry = cached.get();
            if (!entry.active() || isExpired(entry.expiresAt())) {
                throw new UrlExpiredException("This short URL has expired.");
            }
            return entry.originalUrl();
        }
        Url url = urlRepository.resolve(codeOrAlias)
                .orElseThrow(() -> new UrlNotFoundException("No URL exists for short code " + codeOrAlias));
        if (!url.isActive() || url.isExpired()) {
            throw new UrlExpiredException("This short URL has expired.");
        }
        urlCacheService.put(url.getShortCode(), cached(url));
        return url.getOriginalUrl();
    }

    /**
     * Returns a single URL by database id.
     *
     * @param id the database identifier
     * @return the public view of the link
     * @throws UrlNotFoundException when the id is unknown
     */
    @Transactional(readOnly = true)
    public UrlResponse getUrl(Long id) {
        Url url = urlRepository.findById(id)
                .orElseThrow(() -> new UrlNotFoundException("No URL exists for id " + id));
        return toResponse(url);
    }

    /**
     * Soft-deactivates a URL so it no longer redirects.
     *
     * <p>The row and its analytics are retained.
     *
     * @param id the database identifier
     * @throws UrlNotFoundException when the id is unknown
     */
    @Transactional
    public void deleteUrl(Long id) {
        Url url = urlRepository.findById(id)
                .orElseThrow(() -> new UrlNotFoundException("No URL exists for id " + id));
        url.deactivate();
        urlRepository.save(url);
        urlCacheService.evict(url.getShortCode());
        if (url.getCustomAlias() != null && !url.getCustomAlias().equals(url.getShortCode())) {
            urlCacheService.evict(url.getCustomAlias());
        }
        logger.info("Deactivated short URL {}", url.getShortCode());
    }

    /**
     * Claims a custom alias after verifying the shared namespace is free.
     *
     * @param customAlias the requested alias
     * @return the alias as the effective lookup key
     * @throws AliasAlreadyExistsException when the alias or an identical short code exists
     */
    private String claimAlias(String customAlias) {
        if (urlRepository.existsByCustomAlias(customAlias) || urlRepository.existsByShortCode(customAlias)) {
            throw new AliasAlreadyExistsException("Alias already exists: " + customAlias);
        }
        return customAlias;
    }

    /**
     * Generates a random code that is free in both code and alias columns.
     *
     * @return an unused short code
     * @throws AliasAlreadyExistsException when no free code is found within the retry budget
     */
    private String generateUniqueCode() {
        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
            String candidate = shortCodeGenerator.generate();
            if (!urlRepository.existsByShortCode(candidate) && !urlRepository.existsByCustomAlias(candidate)) {
                return candidate;
            }
        }
        throw new AliasAlreadyExistsException("Could not generate a unique short code, please retry");
    }

    /**
     * Maps a persistent entity to its cacheable form.
     *
     * @param url the entity to cache
     * @return the corresponding cache record
     */
    private UrlCacheService.CachedUrl cached(Url url) {
        return new UrlCacheService.CachedUrl(url.getOriginalUrl(), url.getExpiresAt(), url.isActive());
    }

    /**
     * Returns true when the timestamp is set and lies before now.
     *
     * @param expiresAt the timestamp to test, may be null
     * @return true for past timestamps
     */
    private boolean isExpired(Instant expiresAt) {
        return expiresAt != null && expiresAt.isBefore(Instant.now());
    }

    /**
     * Maps a persistent entity to its public view.
     *
     * @param url the entity to expose
     * @return the corresponding response record
     */
    private UrlResponse toResponse(Url url) {
        return new UrlResponse(
                url.getId(),
                url.getShortCode(),
                baseUrl + "/" + url.getShortCode(),
                url.getOriginalUrl(),
                url.getExpiresAt(),
                url.isActive(),
                url.getCreatedAt());
    }
}
