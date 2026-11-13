package com.nlu.shared.infrastructure.cache;

/**
 * Domain-neutral event published whenever a hot-cache rule is actively invalidated.
 */
public record HotCacheInvalidatedEvent(String ruleId) {
}

