package com.shortly.app.controller;

import com.shortly.app.dto.UrlResponse;
import com.shortly.app.exception.InvalidAliasException;
import com.shortly.app.exception.UrlNotFoundException;
import com.shortly.app.service.UrlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice tests for URL management validation and error mapping.
 *
 * <p>Boot 4 carries no mock-bean annotations, so the service mock is supplied
 * as an explicit test bean.
 */
@WebMvcTest(UrlController.class)
class UrlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UrlService urlService;

    /**
     * Resets the shared mock before each test so stubbings never leak
     * across test methods through the cached application context.
     */
    @BeforeEach
    void resetServiceMock() {
        Mockito.reset(urlService);
    }

    /**
     * Supplies the mocked service collaborator.
     */
    @TestConfiguration
    static class MockConfig {

        /**
         * Creates the UrlService mock.
         *
         * @return Mockito mock of the service
         */
        @Bean
        UrlService urlService() {
            return Mockito.mock(UrlService.class);
        }
    }

    /**
     * Verifies successful creation returns 201 with the response body.
     */
    @Test
    void createUrl_returns201WithBody() throws Exception {
        UrlResponse response = new UrlResponse(1L, "abc1234", "http://localhost:8080/abc1234",
                "https://spring.io", null, true, Instant.now());
        given(urlService.createUrl(any())).willReturn(response);

        mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"originalUrl\":\"https://spring.io\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode").value("abc1234"));
    }

    /**
     * Verifies malformed URLs fail validation with field details.
     */
    @Test
    void createUrl_returns400WithValidationErrorsForBadUrl() throws Exception {
        mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"originalUrl\":\"not-a-url\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.validationErrors").isArray());
    }

    /**
     * Verifies reserved aliases map to a 400 problem payload.
     */
    @Test
    void createUrl_returns400ForReservedAlias() throws Exception {
        given(urlService.createUrl(any())).willThrow(new InvalidAliasException("reserved"));

        mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"originalUrl\":\"https://google.com\",\"customAlias\":\"api\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_ALIAS"));
    }

    /**
     * Verifies unknown ids map to a 404 problem payload.
     */
    @Test
    void getUrl_returns404ForUnknownId() throws Exception {
        given(urlService.getUrl(999L)).willThrow(new UrlNotFoundException("missing"));

        mockMvc.perform(get("/api/urls/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("URL_NOT_FOUND"));
    }

    /**
     * Verifies deletion returns 204.
     */
    @Test
    void deleteUrl_returns204() throws Exception {
        mockMvc.perform(delete("/api/urls/1"))
                .andExpect(status().isNoContent());
    }
}
