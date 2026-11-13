package com.nlu.shared.infrastructure.cache;

import java.util.Set;

/**
 * Shared constants for Redis hot-GET HTTP response caching.
 */
public final class HotCacheConstants {

    private HotCacheConstants() {
    }

    public static final String HEADER_CACHE_STATUS = "X-Cache";
    public static final String CACHE_HIT = "HIT";
    public static final String CACHE_MISS_STORED = "MISS-STORED";
    public static final String CACHE_COUNTING = "COUNTING";
    public static final String CACHE_BYPASS = "BYPASS";

    public static final String KEY_PREFIX_COUNTER = "http_cache:counter:";
    public static final String KEY_PREFIX_ENTRY = "http_cache:entry:";
    public static final String KEY_PREFIX_INDEX = "http_cache:index:";
    public static final String KEY_PREFIX_LOCK = "http_cache:lock:";

    public static final int DEFAULT_THRESHOLD = 5;
    public static final long DEFAULT_WINDOW_SECONDS = 60L;
    public static final long DEFAULT_TTL_SECONDS = 300L;
    public static final int DEFAULT_MAX_ENTRIES = 100;
    public static final int DEFAULT_MAX_BODY_BYTES = 65_536;
    public static final int DEFAULT_GZIP_THRESHOLD_BYTES = 2_048;
    public static final long DEFAULT_LOCK_TTL_SECONDS = 5L;
    public static final long DEFAULT_SINGLE_FLIGHT_WAIT_MS = 50L;
    public static final int DEFAULT_SINGLE_FLIGHT_MAX_RETRIES = 3;
    public static final int DEFAULT_MAX_PAGE = 10;
    public static final int DEFAULT_MAX_SIZE = 50;

    public static final byte ENCODING_IDENTITY = 0;
    public static final byte ENCODING_GZIP = 1;

    public static final Set<String> MUTATING_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    public static final Set<String> IGNORED_QUERY_PARAMS = Set.of(
            "utm_source",
            "utm_medium",
            "utm_campaign",
            "utm_term",
            "utm_content",
            "fbclid",
            "gclid",
            "_t",
            "timestamp"
    );
}

