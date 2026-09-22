package com.shortly.app.exception;

/**
 * Thrown when no URL exists for the requested short code, alias or id.
 */
public class UrlNotFoundException extends RuntimeException {

    /**
     * Creates the exception with a detail message.
     *
     * @param message human-readable description of the missing resource
     */
    public UrlNotFoundException(String message) {
        super(message);
    }
}
