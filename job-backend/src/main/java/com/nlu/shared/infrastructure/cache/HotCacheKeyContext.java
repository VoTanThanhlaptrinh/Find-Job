package com.nlu.shared.infrastructure.cache;

/**
 * Resolved Redis keys and metadata for a single cacheable HTTP GET request.
 */
public record HotCacheKeyContext(
        HotCacheProperties.Rule rule,
        String scopeSegment,
        String entryKey,
        String counterKey,
        String lockKey,
        String indexKey
) {
}

