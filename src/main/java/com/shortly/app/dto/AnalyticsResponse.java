package com.shortly.app.dto;

import java.util.List;

/**
 * Aggregated analytics for one shortened URL.
 *
 * @param totalClicks total recorded clicks
 * @param clicksByDate per-day breakdown, oldest first
 * @param clicksByCountry per-country breakdown, most clicks first
 */
public record AnalyticsResponse(
        long totalClicks,
        List<ClickByDateResponse> clicksByDate,
        List<ClickByCountryResponse> clicksByCountry
) {
}
