package com.nlu.shared.infrastructure.cache;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties under {@code app.hot-cache} controlling hot-GET caching,
 * counting thresholds, ZSET quotas, Gzip compression, single-flight locking, and write invalidation.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.hot-cache")
public class HotCacheProperties {

    private boolean enabled = true;
    private int defaultThreshold = HotCacheConstants.DEFAULT_THRESHOLD;
    private long defaultWindowSeconds = HotCacheConstants.DEFAULT_WINDOW_SECONDS;
    private int maxBodyBytes = HotCacheConstants.DEFAULT_MAX_BODY_BYTES;
    private int gzipThresholdBytes = HotCacheConstants.DEFAULT_GZIP_THRESHOLD_BYTES;
    private long lockTtlSeconds = HotCacheConstants.DEFAULT_LOCK_TTL_SECONDS;
    private long singleFlightWaitMs = HotCacheConstants.DEFAULT_SINGLE_FLIGHT_WAIT_MS;
    private int singleFlightMaxWaitRetries = HotCacheConstants.DEFAULT_SINGLE_FLIGHT_MAX_RETRIES;
    private int maxPage = HotCacheConstants.DEFAULT_MAX_PAGE;
    private int maxSize = HotCacheConstants.DEFAULT_MAX_SIZE;
    private List<String> excludedPatterns = new ArrayList<>();
    private List<Rule> rules = new ArrayList<>();

    /**
     * Per-endpoint caching and invalidation configuration.
     */
    @Getter
    @Setter
    public static class Rule {
        private String id;
        private String pattern;
        private HotCacheScope scope = HotCacheScope.PUBLIC;
        private Integer threshold;
        private Long windowSeconds;
        private long ttlSeconds = HotCacheConstants.DEFAULT_TTL_SECONDS;
        private int maxEntries = HotCacheConstants.DEFAULT_MAX_ENTRIES;
        private List<String> invalidateOn = new ArrayList<>();
    }
}

