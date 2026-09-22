package com.shortly.app.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Index;

import java.time.Instant;
import java.util.Objects;

/**
 * Persistent record of a shortened URL.
 *
 * <p>The {@code shortCode} is the generated lookup key while the optional
 * {@code customAlias} offers a human-readable alternative. Both share the
 * {@code GET /{codeOrAlias}} redirect namespace and are therefore each
 * protected by a database-level unique constraint, which remains the final
 * guard against concurrent-insert races.
 */
@Entity
@Table(
        name = "urls",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_urls_short_code", columnNames = "short_code"),
                @UniqueConstraint(name = "uk_urls_custom_alias", columnNames = "custom_alias")
        },
        indexes = {
                @Index(name = "idx_urls_expires_at", columnList = "expires_at"),
                @Index(name = "idx_urls_active", columnList = "active")
        }
)
public class Url {

    /**
     * Surrogate primary key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Destination URL the short link redirects to.
     */
    @Column(name = "original_url", nullable = false, length = 2048)
    private String originalUrl;

    /**
     * Generated lookup key, e.g. {@code xK9a1Z}.
     */
    @Column(name = "short_code", nullable = false, length = 32)
    private String shortCode;

    /**
     * Optional human-readable alias, e.g. {@code my-github}. Null when unused;
     * PostgreSQL treats distinct NULLs as non-conflicting.
     */
    @Column(name = "custom_alias", length = 30)
    private String customAlias;

    /**
     * Creation timestamp, assigned once and never updated.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Optional expiration timestamp. Null means the link never expires.
     */
    @Column(name = "expires_at")
    private Instant expiresAt;

    /**
     * Whether the link currently redirects. Expired or deleted links are
     * deactivated instead of being physically removed so analytics survive.
     */
    @Column(name = "active", nullable = false)
    private boolean active = true;

    /**
     * Creates an empty instance for JPA.
     */
    protected Url() {
    }

    /**
     * Creates a fully initialised instance.
     *
     * @param originalUrl destination URL, must not be blank
     * @param shortCode generated lookup key, must not be blank
     * @param customAlias optional alias, may be null
     * @param expiresAt optional expiration timestamp, may be null
     */
    private Url(String originalUrl, String shortCode, String customAlias, Instant expiresAt) {
        this.originalUrl = originalUrl;
        this.shortCode = shortCode;
        this.customAlias = customAlias;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
        this.active = true;
    }

    /**
     * Creates a new {@code Url} with the current timestamp.
     *
     * @param originalUrl destination URL, must not be blank
     * @param shortCode generated lookup key, must not be blank
     * @param customAlias optional alias, may be null
     * @param expiresAt optional expiration timestamp, may be null
     * @return the new persistent-ready instance
     */
    public static Url create(String originalUrl, String shortCode, String customAlias, Instant expiresAt) {
        return new Url(originalUrl, shortCode, customAlias, expiresAt);
    }

    /**
     * Applies insertion defaults for rows built outside the factory.
     */
    @PrePersist
    protected void onPrePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    /**
     * Returns the surrogate primary key.
     *
     * @return the id, null before persistence
     */
    public Long getId() {
        return id;
    }

    /**
     * Returns the destination URL.
     *
     * @return the original URL
     */
    public String getOriginalUrl() {
        return originalUrl;
    }

    /**
     * Returns the generated lookup key.
     *
     * @return the short code
     */
    public String getShortCode() {
        return shortCode;
    }

    /**
     * Returns the optional human-readable alias.
     *
     * @return the custom alias, or null when unused
     */
    public String getCustomAlias() {
        return customAlias;
    }

    /**
     * Returns the creation timestamp.
     *
     * @return when this record was created
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Returns the optional expiration timestamp.
     *
     * @return when this link expires, or null for never-expiring links
     */
    public Instant getExpiresAt() {
        return expiresAt;
    }

    /**
     * Returns whether this link currently redirects.
     *
     * @return true while the link is usable
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Returns true when this link is past its expiration timestamp.
     *
     * @return true if {@code expiresAt} is set and lies before now
     */
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(Instant.now());
    }

    /**
     * Deactivates this link so it no longer redirects.
     *
     * <p>The row is retained for historical analytics.
     */
    public void deactivate() {
        this.active = false;
    }

    /**
     * Compares instances by the immutable natural key {@code shortCode}.
     *
     * @param other the reference object with which to compare
     * @return true when both instances carry the same short code
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Url that)) {
            return false;
        }
        return Objects.equals(shortCode, that.shortCode);
    }

    /**
     * Returns the hash code derived from {@code shortCode}.
     *
     * @return hash code of the short code
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(shortCode);
    }
}
