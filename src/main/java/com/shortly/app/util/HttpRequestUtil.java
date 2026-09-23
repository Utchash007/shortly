package com.shortly.app.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Extracts client metadata from servlet requests for analytics.
 *
 * <p>{@code X-Forwarded-For} is only meaningful behind a trusted proxy such as
 * Render or Nginx: the first entry is the original client, the rest are proxy
 * hops. Never treat a client-supplied header as authoritative on the open
 * internet.
 */
public final class HttpRequestUtil {

    /**
     * Header carrying the original client IP behind proxies.
     */
    public static final String FORWARDED_FOR = "X-Forwarded-For";

    /**
     * Header carrying the client IP set by some proxies.
     */
    public static final String REAL_IP = "X-Real-IP";

    /**
     * Prevents instantiation.
     */
    private HttpRequestUtil() {
    }

    /**
     * Resolves the best-effort client IP: first {@code X-Forwarded-For} entry,
     * then {@code X-Real-IP}, then the remote address.
     *
     * @param request the current request
     * @return the client IP, never null
     */
    public static String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader(FORWARDED_FOR);
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader(REAL_IP);
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Returns the User-Agent header.
     *
     * @param request the current request
     * @return the user agent, may be null
     */
    public static String getUserAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }

    /**
     * Returns the referrer header.
     *
     * @param request the current request
     * @return the referrer, may be null
     */
    public static String getReferrer(HttpServletRequest request) {
        return request.getHeader("Referer");
    }
}
