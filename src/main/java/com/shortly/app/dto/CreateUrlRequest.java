package com.shortly.app.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.URL;

import java.time.Instant;

/**
 * Payload for creating a shortened URL.
 *
 * @param originalUrl destination URL, http(s) only
 * @param customAlias optional readable alias of 3–30 characters; null means a random code is generated
 * @param expiresAt optional expiration timestamp, must lie in the future
 */
public record CreateUrlRequest(

        @NotBlank(message = "originalUrl must not be blank")
        @URL(message = "originalUrl must be a valid URL")
        @Pattern(regexp = "^https?://.+", message = "originalUrl must start with http:// or https://")
        String originalUrl,

        @Pattern(regexp = "^[a-zA-Z0-9-_]{3,30}$",
                message = "customAlias must be 3-30 characters of letters, digits, hyphen or underscore")
        String customAlias,

        @Future(message = "expiresAt must be in the future")
        Instant expiresAt

) {
}
