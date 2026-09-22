package com.shortly.app.exception;

/**
 * Thrown when a requested custom alias (or generated code) is already taken.
 *
 * <p>Maps to HTTP 409 Conflict, including concurrent-insert races surfaced as
 * {@code DataIntegrityViolationException} by the database unique constraint.
 */
public class AliasAlreadyExistsException extends RuntimeException {

    /**
     * Creates the exception with a detail message.
     *
     * @param message human-readable description of the conflict
     */
    public AliasAlreadyExistsException(String message) {
        super(message);
    }
}
