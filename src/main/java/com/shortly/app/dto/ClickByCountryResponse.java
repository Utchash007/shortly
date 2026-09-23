package com.shortly.app.dto;

/**
 * Clicks aggregated for one country.
 *
 * @param country the resolved country, or {@code UNKNOWN}
 * @param clicks how many clicks from that country
 */
public record ClickByCountryResponse(
        String country,
        long clicks
) {
}
