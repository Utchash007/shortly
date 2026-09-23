package com.shortly.app.controller;

import com.shortly.app.dto.AnalyticsResponse;
import com.shortly.app.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Analytics endpoints exposing aggregated click metrics.
 */
@RestController
@RequestMapping("/api/urls")
@Tag(name = "Analytics", description = "Click metrics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    /**
     * Creates the controller with its service collaborator.
     *
     * @param analyticsService analytics aggregation
     */
    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /**
     * Returns aggregated analytics for one link.
     *
     * @param id the database identifier of the link
     * @return totals plus per-date and per-country breakdowns
     */
    @GetMapping("/{id}/analytics")
    @Operation(summary = "Get analytics for a short URL")
    @ApiResponse(responseCode = "200", description = "Analytics returned")
    @ApiResponse(responseCode = "404", description = "Unknown id")
    public AnalyticsResponse getAnalytics(@PathVariable Long id) {
        return analyticsService.getAnalytics(id);
    }
}
