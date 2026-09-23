package com.shortly.app.service;

import com.shortly.app.dto.AnalyticsResponse;
import com.shortly.app.dto.ClickByCountryResponse;
import com.shortly.app.dto.ClickByDateResponse;
import com.shortly.app.entity.ClickEvent;
import com.shortly.app.exception.UrlNotFoundException;
import com.shortly.app.repository.ClickEventRepository;
import com.shortly.app.repository.UrlRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.util.List;

/**
 * Click recording and analytics aggregation.
 *
 * <p>Recording runs on the {@code analyticsExecutor} pool so the redirect
 * response never waits on geolocation or database writes. Aggregation reads
 * the raw {@code click_events} rows; a rollup table is the documented
 * scaling path, not part of this revision.
 */
@Service
public class AnalyticsService {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsService.class);

    private final ClickEventRepository clickEventRepository;
    private final UrlRepository urlRepository;
    private final GeolocationService geolocationService;

    /**
     * Creates the service with its required collaborators.
     *
     * @param clickEventRepository persistence for click events
     * @param urlRepository reference resolution for owning links
     * @param geolocationService IP-to-country resolution
     */
    public AnalyticsService(ClickEventRepository clickEventRepository,
                            UrlRepository urlRepository,
                            GeolocationService geolocationService) {
        this.clickEventRepository = clickEventRepository;
        this.urlRepository = urlRepository;
        this.geolocationService = geolocationService;
    }

    /**
     * Records one click asynchronously.
     *
     * <p>Failures are logged and swallowed: analytics enrichment is
     * best-effort and must never surface to callers.
     *
     * @param urlId owning link id
     * @param ipAddress client IP, may be null
     * @param userAgent User-Agent header, may be null
     * @param referrer referrer header, may be null
     */
    @Async("analyticsExecutor")
    public void recordClickAsync(Long urlId, String ipAddress, String userAgent, String referrer) {
        try {
            String country = geolocationService.lookupCountry(ipAddress);
            clickEventRepository.save(ClickEvent.record(
                    urlRepository.getReferenceById(urlId), ipAddress, country, userAgent, referrer));
        } catch (Exception e) {
            logger.warn("Dropping click event for url {}: {}", urlId, e.toString());
        }
    }

    /**
     * Aggregates analytics for one link.
     *
     * @param urlId owning link id
     * @return totals plus per-date and per-country breakdowns
     * @throws UrlNotFoundException when the id is unknown
     */
    @Transactional(readOnly = true)
    public AnalyticsResponse getAnalytics(Long urlId) {
        if (!urlRepository.existsById(urlId)) {
            throw new UrlNotFoundException("No URL exists for id " + urlId);
        }
        long total = clickEventRepository.countByUrl_Id(urlId);
        List<ClickByDateResponse> byDate = clickEventRepository.countGroupedByDate(urlId).stream()
                .map(row -> new ClickByDateResponse(((Date) row[0]).toLocalDate(), (Long) row[1]))
                .toList();
        List<ClickByCountryResponse> byCountry = clickEventRepository.countGroupedByCountry(urlId).stream()
                .map(row -> new ClickByCountryResponse((String) row[0], (Long) row[1]))
                .toList();
        return new AnalyticsResponse(total, byDate, byCountry);
    }
}
