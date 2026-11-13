package com.nlu.shared.infrastructure.cache;

/**
 * Immutable representation of a cached HTTP response payload decoded from Redis.
 */
public record CachedHttpResponse(
        int httpStatus,
        String contentType,
        byte[] body
) {
}

