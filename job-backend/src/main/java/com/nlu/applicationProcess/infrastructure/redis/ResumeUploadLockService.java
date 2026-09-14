package com.nlu.applicationProcess.infrastructure.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.Optional;

/**
 * Service providing atomic Redis distributed locks with short TTL for resume upload idempotency.
 * Uses atomic SET NX EX and compare-and-delete via Lua script.
 * Degrades gracefully when Redis is unavailable, allowing DB unique constraints to guarantee correctness.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeUploadLockService {

    private final RedisTemplate<String, Object> redisTemplate;

    public static final String LOCK_KEY_PREFIX = "resume-upload:initiate:";
    public static final Duration DEFAULT_LOCK_TTL = Duration.ofSeconds(60);

    private static final String RELEASE_LOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "    return redis.call('del', KEYS[1]) " +
            "else " +
            "    return 0 " +
            "end";

    private final RedisScript<Long> releaseScript = new DefaultRedisScript<>(RELEASE_LOCK_SCRIPT, Long.class);

    /**
     * Attempts to acquire an atomic distributed lock for the specified user and idempotency key.
     *
     * @param userId         The authenticated user ID
     * @param idempotencyKey The client idempotency key
     * @param lockToken      Unique ownership token for this lock attempt
     * @return Optional containing the lockToken if acquired; Optional.empty() if locked or on Redis error
     */
    public Optional<String> acquireLock(long userId, String idempotencyKey, String lockToken) {
        String key = buildLockKey(userId, idempotencyKey);
        try {
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, lockToken, DEFAULT_LOCK_TTL);
            if (Boolean.TRUE.equals(acquired)) {
                log.debug("Acquired Redis lock for key: {}", key);
                return Optional.of(lockToken);
            }
            log.debug("Redis lock already held by another request for key: {}", key);
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Redis outage or error acquiring lock for key {}: {}. Proceeding to database.",
                    key, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Atomically releases the lock only if the current value in Redis matches the provided lockToken.
     *
     * @param userId         The authenticated user ID
     * @param idempotencyKey The client idempotency key
     * @param lockToken      Unique ownership token that acquired the lock
     * @return true if successfully deleted; false otherwise
     */
    public boolean releaseLock(long userId, String idempotencyKey, String lockToken) {
        if (lockToken == null) {
            return false;
        }
        String key = buildLockKey(userId, idempotencyKey);
        try {
            Long result = redisTemplate.execute(releaseScript, Collections.singletonList(key), lockToken);
            boolean released = result != null && result > 0;
            if (released) {
                log.debug("Released Redis lock for key: {}", key);
            }
            return released;
        } catch (Exception e) {
            log.warn("Redis error releasing lock for key {}: {}", key, e.getMessage());
            return false;
        }
    }

    public String buildLockKey(long userId, String idempotencyKey) {
        return LOCK_KEY_PREFIX + userId + ":" + idempotencyKey;
    }
}
