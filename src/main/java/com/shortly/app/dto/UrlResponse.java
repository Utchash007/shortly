package com.shortly.app.dto;

import java.time.Instant;

/**
 * Public view of a shortened URL. The persistence entity is never exposed directly.
 *
 * @param id database identifier
 * @param shortCode lookup key (generated code or custom alias)
 * @param shortUrl fully qualified redirect URL clients visit
 * @param originalUrl destination URL
 * @param expiresAt expiration timestamp, null for never-expiring links
 * @param active whether the link currently redirects
 * @param createdAt creation timestamp
 */
public record UrlResponse(
        Long id,
        String shortCode,
        String shortUrl,
        String originalUrl,
        Instant expiresAt,
        boolean active,
        Instant createdAt
) {
}
