package com.shortly.app.exception;

import com.shortly.app.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.List;

/**
 * Central error mapping for the whole API.
 *
 * <p>Domain exceptions become semantic HTTP statuses with a stable
 * {@link ErrorResponse} shape; unexpected failures stay 500 with the incident
 * logged server-side.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles bean-validation failures on request bodies.
     *
     * @param e the validation failure with field errors
     * @param request the failing request
     * @return 400 with per-field violations
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException e, HttpServletRequest request) {
        List<String> violations = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();
        return ResponseEntity.badRequest().body(new ErrorResponse(
                Instant.now(), HttpStatus.BAD_REQUEST.value(), "VALIDATION_ERROR",
                "Request validation failed", request.getRequestURI(), violations));
    }

    /**
     * Handles constraint violations on path variables and parameters.
     *
     * @param e the violation
     * @param request the failing request
     * @return 400 with per-violation messages
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException e, HttpServletRequest request) {
        List<String> violations = e.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .toList();
        return ResponseEntity.badRequest().body(new ErrorResponse(
                Instant.now(), HttpStatus.BAD_REQUEST.value(), "VALIDATION_ERROR",
                "Request validation failed", request.getRequestURI(), violations));
    }

    /**
     * Handles malformed JSON and mistyped parameters.
     *
     * @param e the mapping failure
     * @param request the failing request
     * @return 400
     */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception e, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(new ErrorResponse(
                Instant.now(), HttpStatus.BAD_REQUEST.value(), "INVALID_REQUEST",
                e.getMessage(), request.getRequestURI(), null));
    }

    /**
     * Handles reserved or malformed custom aliases.
     *
     * @param e the alias violation
     * @param request the failing request
     * @return 400
     */
    @ExceptionHandler(InvalidAliasException.class)
    public ResponseEntity<ErrorResponse> handleInvalidAlias(
            InvalidAliasException e, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(new ErrorResponse(
                Instant.now(), HttpStatus.BAD_REQUEST.value(), "INVALID_ALIAS",
                e.getMessage(), request.getRequestURI(), null));
    }

    /**
     * Handles lookups of unknown codes, aliases and ids.
     *
     * @param e the missing resource
     * @param request the failing request
     * @return 404
     */
    @ExceptionHandler(UrlNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            UrlNotFoundException e, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(
                Instant.now(), HttpStatus.NOT_FOUND.value(), "URL_NOT_FOUND",
                e.getMessage(), request.getRequestURI(), null));
    }

    /**
     * Handles alias collisions, including concurrent-insert races surfaced by
     * the database unique constraint.
     *
     * @param e the conflict
     * @param request the failing request
     * @return 409
     */
    @ExceptionHandler({AliasAlreadyExistsException.class, DataIntegrityViolationException.class})
    public ResponseEntity<ErrorResponse> handleConflict(Exception e, HttpServletRequest request) {
        String message = e instanceof AliasAlreadyExistsException
                ? e.getMessage()
                : "Short code already exists, please retry";
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(
                Instant.now(), HttpStatus.CONFLICT.value(), "ALIAS_ALREADY_EXISTS",
                message, request.getRequestURI(), null));
    }

    /**
     * Handles redirects to deactivated or past-expiry links.
     *
     * @param e the expiry
     * @param request the failing request
     * @return 410
     */
    @ExceptionHandler(UrlExpiredException.class)
    public ResponseEntity<ErrorResponse> handleExpired(
            UrlExpiredException e, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.GONE).body(new ErrorResponse(
                Instant.now(), HttpStatus.GONE.value(), "URL_EXPIRED",
                e.getMessage(), request.getRequestURI(), null));
    }

    /**
     * Handles every unexpected failure.
     *
     * @param e the failure
     * @param request the failing request
     * @return 500 without leaking internals
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e, HttpServletRequest request) {
        logger.error("Unhandled error on {}", request.getRequestURI(), e);
        return ResponseEntity.internalServerError().body(new ErrorResponse(
                Instant.now(), HttpStatus.INTERNAL_SERVER_ERROR.value(), "INTERNAL_ERROR",
                "An unexpected error occurred", request.getRequestURI(), null));
    }
}
