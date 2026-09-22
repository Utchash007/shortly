package com.shortly.app.controller;

import com.shortly.app.service.UrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Redirect endpoint resolving short codes and custom aliases.
 *
 * <p>The path pattern only matches code-safe characters so dotted system paths
 * such as {@code /favicon.ico} never reach the resolver.
 */
@RestController
@Tag(name = "Redirect", description = "Short code resolution")
public class RedirectController {

    private final UrlService urlService;

    /**
     * Creates the controller with its service collaborator.
     *
     * @param urlService core shortening logic
     */
    public RedirectController(UrlService urlService) {
        this.urlService = urlService;
    }

    /**
     * Redirects to the original URL for the given short code or alias.
     *
     * @param shortCode the lookup key from the path
     * @return 302 Found with a {@code Location} header
     */
    @GetMapping("/{shortCode:[a-zA-Z0-9-_]+}")
    @Operation(summary = "Redirect to the original URL")
    @ApiResponse(responseCode = "302", description = "Redirect to original URL")
    @ApiResponse(responseCode = "404", description = "Unknown short code")
    @ApiResponse(responseCode = "410", description = "Expired or deactivated link")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        String originalUrl = urlService.resolveUrl(shortCode);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(originalUrl)).build();
    }
}
