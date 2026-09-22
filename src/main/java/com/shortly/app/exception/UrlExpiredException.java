package com.shortly.app.exception;

/**
 * Thrown when a short link is past its expiration time or has been deactivated.
 *
 * <p>Maps to HTTP 410 Gone.
 */
public class UrlExpiredException extends RuntimeException {

    /**
     * Creates the exception with a detail message.
     *
     * @param message human-readable description of the expiry
     */
    public UrlExpiredException(String message) {
        super(message);
    }
}
