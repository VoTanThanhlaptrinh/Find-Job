package com.job_web.constant;

/**
 * Constants for rate limiting configuration.
 */
public final class RateLimitConstants {

    private RateLimitConstants() {
        // Prevent instantiation
    }

    /**
     * Rate limit for public/unauthenticated requests.
     * 200 requests per minute.
     */
    public static final int PUBLIC_RATE_LIMIT = 200;

    /**
     * Rate limit for authenticated requests.
     * 400 requests per minute.
     */
    public static final int AUTHENTICATED_RATE_LIMIT = 400;

    /**
     * Time window for rate limiting in seconds.
     * Default: 60 seconds (1 minute).
     */
    public static final long WINDOW_SIZE_SECONDS = 60;

    /**
     * Penalty duration when rate limit is exceeded.
     * Default: 300 seconds (5 minutes).
     */
    public static final long PENALTY_DURATION_SECONDS = 5 * 60;

    /**
     * Redis key prefix for rate limit counters.
     */
    public static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:";

    /**
     * Redis key prefix for blocked clients.
     */
    public static final String BLOCK_KEY_PREFIX = "rate_limit_block:";
}
