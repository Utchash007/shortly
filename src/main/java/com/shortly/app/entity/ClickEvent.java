package com.shortly.app.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * One recorded visit to a shortened URL.
 *
 * <p>Clicks are stored as individual rows so analytics stay flexible; the
 * redirect path only ever appends. High-volume aggregation belongs to a
 * future rollup table, not to the hot {@code urls} row.
 */
@Entity
@Table(
        name = "click_events",
        indexes = {
                @Index(name = "idx_click_url", columnList = "url_id"),
                @Index(name = "idx_click_url_date", columnList = "url_id, clicked_at"),
                @Index(name = "idx_click_url_country", columnList = "url_id, country")
        }
)
public class ClickEvent {

    /**
     * Surrogate primary key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The visited link.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "url_id", nullable = false)
    private Url url;

    /**
     * When the visit happened.
     */
    @Column(name = "clicked_at", nullable = false, updatable = false)
    private Instant clickedAt;

    /**
     * Client IP address as observed by the server.
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * Resolved country code, or {@code UNKNOWN} when geolocation fails.
     */
    @Column(name = "country", length = 100)
    private String country;

    /**
     * Client User-Agent header.
     */
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    /**
     * Referrer header when present.
     */
    @Column(name = "referrer", columnDefinition = "TEXT")
    private String referrer;

    /**
     * Creates an empty instance for JPA.
     */
    protected ClickEvent() {
    }

    /**
     * Creates a fully initialised instance.
     *
     * @param url owning link, must not be null
     * @param clickedAt visit timestamp, must not be null
     * @param ipAddress client IP, may be null
     * @param country resolved country, may be null
     * @param userAgent User-Agent header, may be null
     * @param referrer referrer header, may be null
     */
    private ClickEvent(Url url, Instant clickedAt, String ipAddress, String country,
                       String userAgent, String referrer) {
        this.url = url;
        this.clickedAt = clickedAt;
        this.ipAddress = ipAddress;
        this.country = country;
        this.userAgent = userAgent;
        this.referrer = referrer;
    }

    /**
     * Records a click for the given link at the current time.
     *
     * @param url owning link, must not be null
     * @param ipAddress client IP, may be null
     * @param country resolved country, may be null
     * @param userAgent User-Agent header, may be null
     * @param referrer referrer header, may be null
     * @return the new persistent-ready instance
     */
    public static ClickEvent record(Url url, String ipAddress, String country,
                                    String userAgent, String referrer) {
        return new ClickEvent(url, Instant.now(), ipAddress, country, userAgent, referrer);
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
     * Returns the visited link.
     *
     * @return the owning URL
     */
    public Url getUrl() {
        return url;
    }

    /**
     * Returns the visit timestamp.
     *
     * @return when the visit happened
     */
    public Instant getClickedAt() {
        return clickedAt;
    }

    /**
     * Returns the observed client IP.
     *
     * @return the IP address, may be null
     */
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * Returns the resolved country.
     *
     * @return the country, may be null
     */
    public String getCountry() {
        return country;
    }

    /**
     * Returns the User-Agent header.
     *
     * @return the user agent, may be null
     */
    public String getUserAgent() {
        return userAgent;
    }

    /**
     * Returns the referrer header.
     *
     * @return the referrer, may be null
     */
    public String getReferrer() {
        return referrer;
    }
}
