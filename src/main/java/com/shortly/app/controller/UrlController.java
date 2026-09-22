package com.shortly.app.controller;

import com.shortly.app.dto.CreateUrlRequest;
import com.shortly.app.dto.UrlResponse;
import com.shortly.app.service.UrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * URL management endpoints: creation, lookup and soft deletion.
 */
@RestController
@RequestMapping("/api/urls")
@Tag(name = "URLs", description = "Short URL management")
public class UrlController {

    private final UrlService urlService;

    /**
     * Creates the controller with its service collaborator.
     *
     * @param urlService core shortening logic
     */
    public UrlController(UrlService urlService) {
        this.urlService = urlService;
    }

    /**
     * Creates a shortened URL.
     *
     * @param request validated creation payload
     * @return 201 Created with the public view of the link
     */
    @PostMapping
    @Operation(summary = "Create a short URL")
    @ApiResponse(responseCode = "201", description = "Short URL created")
    @ApiResponse(responseCode = "400", description = "Invalid URL, alias or expiration")
    @ApiResponse(responseCode = "409", description = "Custom alias already taken")
    public ResponseEntity<UrlResponse> createUrl(@Valid @RequestBody CreateUrlRequest request) {
        UrlResponse response = urlService.createUrl(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Returns a single shortened URL by database id.
     *
     * @param id the database identifier
     * @return 200 with the public view of the link
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get a short URL by id")
    @ApiResponse(responseCode = "200", description = "Short URL found")
    @ApiResponse(responseCode = "404", description = "Unknown id")
    public UrlResponse getUrl(@PathVariable Long id) {
        return urlService.getUrl(id);
    }

    /**
     * Soft-deactivates a shortened URL so it no longer redirects.
     *
     * @param id the database identifier
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Deactivate a short URL")
    @ApiResponse(responseCode = "204", description = "Short URL deactivated")
    @ApiResponse(responseCode = "404", description = "Unknown id")
    public void deleteUrl(@PathVariable Long id) {
        urlService.deleteUrl(id);
    }
}
