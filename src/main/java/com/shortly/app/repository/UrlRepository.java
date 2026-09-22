package com.shortly.app.repository;

import com.shortly.app.entity.Url;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Persistence operations for {@link Url} records.
 *
 * <p>Derived query methods keep lookups index-friendly against
 * {@code short_code}, {@code custom_alias} and {@code expires_at}.
 */
public interface UrlRepository extends JpaRepository<Url, Long> {

    /**
     * Finds a URL by its generated short code.
     *
     * @param shortCode the generated lookup key
     * @return the matching URL, or empty when unknown
     */
    Optional<Url> findByShortCode(String shortCode);

    /**
     * Finds a URL by its custom alias.
     *
     * @param customAlias the human-readable alias
     * @return the matching URL, or empty when unknown
     */
    Optional<Url> findByCustomAlias(String customAlias);

    /**
     * Resolves either a short code or a custom alias through the shared
     * {@code GET /{codeOrAlias}} redirect namespace.
     *
     * @param codeOrAlias the short code or alias supplied in the path
     * @return the matching URL, or empty when unknown
     */
    default Optional<Url> resolve(String codeOrAlias) {
        return findByShortCode(codeOrAlias).or(() -> findByCustomAlias(codeOrAlias));
    }

    /**
     * Checks whether a short code is already taken.
     *
     * @param shortCode the candidate short code
     * @return true when the short code exists
     */
    boolean existsByShortCode(String shortCode);

    /**
     * Checks whether a custom alias is already taken.
     *
     * @param customAlias the candidate alias
     * @return true when the alias exists
     */
    boolean existsByCustomAlias(String customAlias);

    /**
     * Finds active URLs whose expiration timestamp has passed.
     *
     * <p>Consumed by the scheduled cleanup job to soft-deactivate stale links.
     *
     * @param now the reference timestamp, typically {@link Instant#now()}
     * @return expired but still active URLs
     */
    List<Url> findByExpiresAtBeforeAndActiveTrue(Instant now);
}
