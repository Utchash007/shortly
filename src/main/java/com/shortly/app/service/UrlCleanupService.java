package com.shortly.app.service;

import com.shortly.app.entity.Url;
import com.shortly.app.repository.UrlRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Background cleanup for expired short URLs.
 *
 * <p>Runs at the top of every hour: expired but still active rows are
 * soft-deactivated (analytics and history survive) and their Redis entries are
 * evicted so no stale redirect can be served from cache. Redirect-time checks
 * in {@link UrlService} already return 410 Gone; this job only reclaims
 * resolvability consistently across DB and cache.
 */
@Service
public class UrlCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(UrlCleanupService.class);

    private final UrlRepository urlRepository;
    private final UrlCacheService urlCacheService;

    /**
     * Creates the service with its required collaborators.
     *
     * @param urlRepository persistence for URL records
     * @param urlCacheService cache eviction for deactivated links
     */
    public UrlCleanupService(UrlRepository urlRepository, UrlCacheService urlCacheService) {
        this.urlRepository = urlRepository;
        this.urlCacheService = urlCacheService;
    }

    /**
     * Deactivates all links whose expiration time has passed.
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void deactivateExpiredUrls() {
        List<Url> expired = urlRepository.findByExpiresAtBeforeAndActiveTrue(Instant.now());
        if (expired.isEmpty()) {
            logger.debug("No expired URLs to deactivate");
            return;
        }
        List<String> keys = new ArrayList<>();
        for (Url url : expired) {
            url.deactivate();
            keys.add(url.getShortCode());
            if (url.getCustomAlias() != null && !url.getCustomAlias().equals(url.getShortCode())) {
                keys.add(url.getCustomAlias());
            }
        }
        urlRepository.saveAll(expired);
        urlCacheService.evict(keys.toArray(String[]::new));
        logger.info("Deactivated {} expired URLs", expired.size());
    }
}
