package com.shortly.app.service;

import com.shortly.app.entity.Url;
import com.shortly.app.repository.UrlRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for scheduled expiration cleanup.
 */
@ExtendWith(MockitoExtension.class)
class UrlCleanupServiceTest {

    @Mock
    private UrlRepository urlRepository;

    @Mock
    private UrlCacheService urlCacheService;

    @InjectMocks
    private UrlCleanupService cleanupService;

    /**
     * Verifies expired links are deactivated, saved and evicted.
     */
    @Test
    void deactivateExpiredUrls_deactivatesSavesAndEvicts() {
        Url expired = Url.create("https://example.com", "expcode1", null,
                Instant.now().minusSeconds(60));
        when(urlRepository.findByExpiresAtBeforeAndActiveTrue(any()))
                .thenReturn(List.of(expired));

        cleanupService.deactivateExpiredUrls();

        assertFalse(expired.isActive());
        verify(urlRepository).saveAll(List.of(expired));
        verify(urlCacheService).evict("expcode1");
    }

    /**
     * Verifies a clean run touches nothing.
     */
    @Test
    void deactivateExpiredUrls_skipsWorkWhenNothingExpired() {
        when(urlRepository.findByExpiresAtBeforeAndActiveTrue(any()))
                .thenReturn(List.of());

        cleanupService.deactivateExpiredUrls();

        verify(urlRepository, never()).saveAll(anyList());
        verify(urlCacheService, never()).evict();
    }
}
