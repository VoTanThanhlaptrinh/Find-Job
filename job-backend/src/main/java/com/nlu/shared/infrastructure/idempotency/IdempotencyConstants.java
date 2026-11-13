package com.nlu.shared.infrastructure.idempotency;

import java.util.List;
import java.util.Set;

public final class IdempotencyConstants {

    private IdempotencyConstants() {
    }

    public static final String HEADER_IDEMPOTENCY_KEY = "Idempotency-Key";
    public static final String HEADER_IDEMPOTENT_REPLAYED = "X-Idempotent-Replayed";
    public static final String REDIS_KEY_PREFIX = "idempotency:";
    public static final int MAX_KEY_LENGTH = 128;
    public static final int MAX_CACHEABLE_BODY_BYTES = 65_536;

    public static final Set<String> DEFAULT_MUTATING_METHODS = Set.of("POST", "PUT", "PATCH");

    public static final List<String> DEFAULT_ENDPOINTS = List.of(
            "POST /api/payments",
            "POST /api/payments/**"
    );
}

