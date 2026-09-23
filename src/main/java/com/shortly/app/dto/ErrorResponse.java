package com.shortly.app.dto;

import java.time.Instant;
import java.util.List;

/**
 * Consistent error payload for every failure response.
 *
 * <p>{@code validationErrors} carries per-field messages for bean-validation
 * failures and is null for all other error types.
 *
 * @param timestamp when the error occurred
 * @param status HTTP status code
 * @param error machine-readable error code, e.g. {@code URL_NOT_FOUND}
 * @param message human-readable description
 * @param path request path that failed
 * @param validationErrors per-field violations, null unless validation failed
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<String> validationErrors
) {
}
