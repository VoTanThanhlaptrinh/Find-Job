package com.nlu.shared.infrastructure.cache;

/**
 * Isolation scope for cached HTTP GET responses.
 */
public enum HotCacheScope {
    PUBLIC,
    ROLE,
    USER
}

