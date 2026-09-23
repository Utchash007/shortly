package com.shortly.app.service;

import com.shortly.app.dto.CreateUrlRequest;
import com.shortly.app.dto.UrlResponse;
import com.shortly.app.entity.Url;
import com.shortly.app.exception.AliasAlreadyExistsException;
import com.shortly.app.exception.InvalidAliasException;
import com.shortly.app.exception.UrlExpiredException;
import com.shortly.app.exception.UrlNotFoundException;
import com.shortly.app.repository.UrlRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for URL creation, resolution and deletion, including cache
 * interaction contracts.
 */
@ExtendWith(MockitoExtension.class)
class UrlServiceTest {

    @Mock
    private UrlRepository urlRepository;

    @Mock
    private ShortCodeGenerator shortCodeGenerator;

    @Mock
    private UrlCacheService urlCacheService;

    @InjectMocks
    private UrlService urlService;

    /**
     * Verifies creation generates a code, persists and warms the cache.
     */
    @Test
    void createUrl_generatesCodeAndWarmsCache() {
        when(shortCodeGenerator.generate()).thenReturn("abc1234");
        when(urlRepository.existsByShortCode("abc1234")).thenReturn(false);
        when(urlRepository.existsByCustomAlias("abc1234")).thenReturn(false);
        when(urlRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        UrlResponse response = urlService.createUrl(
                new CreateUrlRequest("https://spring.io", null, null));

        assertEquals("abc1234", response.shortCode());
        assertEquals("https://spring.io", response.originalUrl());
        verify(urlCacheService).put(any(), any());
    }

    /**
     * Verifies duplicate aliases are rejected without touching the database.
     */
    @Test
    void createUrl_rejectsDuplicateAlias() {
        when(urlRepository.existsByCustomAlias("taken")).thenReturn(true);

        assertThrows(AliasAlreadyExistsException.class, () -> urlService.createUrl(
                new CreateUrlRequest("https://google.com", "taken", null)));
        verify(urlRepository, never()).save(any());
    }

    /**
     * Verifies reserved system paths are rejected.
     */
    @Test
    void createUrl_rejectsReservedAlias() {
        assertThrows(InvalidAliasException.class, () -> urlService.createUrl(
                new CreateUrlRequest("https://google.com", "api", null)));
    }

    /**
     * Verifies already-expired payloads are rejected.
     */
    @Test
    void createUrl_rejectsPastExpiration() {
        assertThrows(IllegalArgumentException.class, () -> urlService.createUrl(
                new CreateUrlRequest("https://google.com", null, Instant.now().minusSeconds(60))));
    }

    /**
     * Verifies a cache hit resolves without any database access.
     */
    @Test
    void resolveUrl_returnsCachedUrlWithoutDatabase() {
        when(urlCacheService.get("cached1")).thenReturn(Optional.of(
                new UrlCacheService.CachedUrl("https://example.com", null, true, 42L)));

        assertEquals("https://example.com", urlService.resolveRedirect("cached1").originalUrl());
        assertEquals(42L, urlService.resolveRedirect("cached1").urlId());
        verify(urlRepository, never()).resolve(any());
    }

    /**
     * Verifies a cache miss falls back to the database and repopulates.
     */
    @Test
    void resolveUrl_fallsBackToDatabaseAndRepopulates() {
        Url url = Url.create("https://example.com", "dbcode1", null, null);
        when(urlCacheService.get("dbcode1")).thenReturn(Optional.empty());
        when(urlRepository.resolve("dbcode1")).thenReturn(Optional.of(url));

        assertEquals("https://example.com", urlService.resolveRedirect("dbcode1").originalUrl());
        verify(urlCacheService).put(any(), any());
    }

    /**
     * Verifies expired links resolve to an expiry failure.
     */
    @Test
    void resolveUrl_throwsForExpiredUrl() {
        Url url = Url.create("https://example.com", "oldcode1", null, Instant.now().minusSeconds(60));
        when(urlCacheService.get("oldcode1")).thenReturn(Optional.empty());
        when(urlRepository.resolve("oldcode1")).thenReturn(Optional.of(url));

        assertThrows(UrlExpiredException.class, () -> urlService.resolveRedirect("oldcode1"));
    }

    /**
     * Verifies unknown codes resolve to a not-found failure.
     */
    @Test
    void resolveUrl_throwsForUnknownCode() {
        when(urlCacheService.get("nope123")).thenReturn(Optional.empty());
        when(urlRepository.resolve("nope123")).thenReturn(Optional.empty());

        assertThrows(UrlNotFoundException.class, () -> urlService.resolveRedirect("nope123"));
    }

    /**
     * Verifies deletion deactivates and evicts the cache entry.
     */
    @Test
    void deleteUrl_deactivatesAndEvicts() {
        Url url = Url.create("https://example.com", "delcode1", null, null);
        when(urlRepository.findById(7L)).thenReturn(Optional.of(url));

        urlService.deleteUrl(7L);

        verify(urlRepository).save(url);
        verify(urlCacheService).evict("delcode1");
    }
}
