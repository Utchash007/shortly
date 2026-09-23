package com.shortly.app.dto;

import java.time.LocalDate;

/**
 * Clicks aggregated on one calendar date.
 *
 * @param date the day of the clicks
 * @param clicks how many clicks that day
 */
public record ClickByDateResponse(
        LocalDate date,
        long clicks
) {
}
