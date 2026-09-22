package com.shortly.app.exception;

/**
 * Thrown when a custom alias uses illegal characters or a reserved system path.
 *
 * <p>Maps to HTTP 400 Bad Request.
 */
public class InvalidAliasException extends RuntimeException {

    /**
     * Creates the exception with a detail message.
     *
     * @param message human-readable description of the violation
     */
    public InvalidAliasException(String message) {
        super(message);
    }
}
