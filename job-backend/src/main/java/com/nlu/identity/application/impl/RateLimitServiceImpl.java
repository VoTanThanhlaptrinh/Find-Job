package com.nlu.identity.application.impl;

import com.nlu.identity.domain.vo.RateLimitConstants;
import com.nlu.identity.application.RateLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Redis-based implementation of RateLimitService.
 * Uses sliding window algorithm for rate limiting.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitServiceImpl implements RateLimitService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public int consumeAndGetRemaining(String clientId, int limit) {
        long startNs = System.nanoTime();
        String key = RateLimitConstants.RATE_LIMIT_KEY_PREFIX + clientId;
        long incrStartNs = System.nanoTime();
        Long currentCount = redisTemplate.opsForValue().increment(key);
        long redisIncrementMs = (System.nanoTime() - incrStartNs) / 1_000_000L;

        if (currentCount == null) {
            log.info("[PERF][Service:Redis][RateLimitService] consumeAndGetRemaining client={} count=null | total={}ms (redisIncrement={}ms)",
                    clientId, (System.nanoTime() - startNs) / 1_000_000L, redisIncrementMs);
            return -1;
        }

        long redisExpireMs = 0;
        if (currentCount == 1) {
            long expireStartNs = System.nanoTime();
            redisTemplate.expire(key, RateLimitConstants.WINDOW_SIZE_SECONDS, TimeUnit.SECONDS);
            redisExpireMs = (System.nanoTime() - expireStartNs) / 1_000_000L;
        }

        if (currentCount > limit) {
            blockClient(clientId);
            log.warn("Rate limit exceeded for client: {}. Blocked for {} minutes.",
                    clientId, RateLimitConstants.PENALTY_DURATION_SECONDS / 60);
            log.info("[PERF][Service:Redis][RateLimitService] consumeAndGetRemaining client={} exceeded=true | total={}ms (redisIncrement={}ms, redisExpire={}ms)",
                    clientId, (System.nanoTime() - startNs) / 1_000_000L, redisIncrementMs, redisExpireMs);
            return -1;
        }

        int remaining = Math.max(0, limit - currentCount.intValue());
        log.info("[PERF][Service:Redis][RateLimitService] consumeAndGetRemaining client={} count={} remaining={} | total={}ms (redisIncrement={}ms, redisExpire={}ms)",
                clientId, currentCount, remaining, (System.nanoTime() - startNs) / 1_000_000L, redisIncrementMs, redisExpireMs);
        return remaining;
    }

    @Override
    public boolean isAllowed(String clientId, int limit) {
        long startNs = System.nanoTime();
        long blockedStartNs = System.nanoTime();
        boolean blocked = isBlocked(clientId);
        long redisIsBlockedMs = (System.nanoTime() - blockedStartNs) / 1_000_000L;
        if (blocked) {
            log.info("[PERF][Service:Redis][RateLimitService] isAllowed client={} blocked=true | total={}ms (redisHasKey={}ms)",
                    clientId, (System.nanoTime() - startNs) / 1_000_000L, redisIsBlockedMs);
            return false;
        }

        String key = RateLimitConstants.RATE_LIMIT_KEY_PREFIX + clientId;
        long incrStartNs = System.nanoTime();
        Long currentCount = redisTemplate.opsForValue().increment(key);
        long redisIncrementMs = (System.nanoTime() - incrStartNs) / 1_000_000L;

        if (currentCount == null) {
            log.info("[PERF][Service:Redis][RateLimitService] isAllowed client={} count=null | total={}ms (redisHasKey={}ms, redisIncrement={}ms)",
                    clientId, (System.nanoTime() - startNs) / 1_000_000L, redisIsBlockedMs, redisIncrementMs);
            return false;
        }

        long redisExpireMs = 0;
        // Set expiry on first request
        if (currentCount == 1) {
            long expireStartNs = System.nanoTime();
            redisTemplate.expire(key, RateLimitConstants.WINDOW_SIZE_SECONDS, TimeUnit.SECONDS);
            redisExpireMs = (System.nanoTime() - expireStartNs) / 1_000_000L;
        }

        // Check if limit exceeded
        if (currentCount > limit) {
            blockClient(clientId);
            log.warn("Rate limit exceeded for client: {}. Blocked for {} minutes.", 
                    clientId, RateLimitConstants.PENALTY_DURATION_SECONDS / 60);
            log.info("[PERF][Service:Redis][RateLimitService] isAllowed client={} exceeded=true | total={}ms (redisHasKey={}ms, redisIncrement={}ms, redisExpire={}ms)",
                    clientId, (System.nanoTime() - startNs) / 1_000_000L, redisIsBlockedMs, redisIncrementMs, redisExpireMs);
            return false;
        }

        log.info("[PERF][Service:Redis][RateLimitService] isAllowed client={} count={} | total={}ms (redisHasKey={}ms, redisIncrement={}ms, redisExpire={}ms)",
                clientId, currentCount, (System.nanoTime() - startNs) / 1_000_000L, redisIsBlockedMs, redisIncrementMs, redisExpireMs);
        return true;
    }

    @Override
    public boolean isBlocked(String clientId) {
        String blockKey = RateLimitConstants.BLOCK_KEY_PREFIX + clientId;
        Boolean exists = redisTemplate.hasKey(blockKey);
        return Boolean.TRUE.equals(exists);
    }

    @Override
    public void blockClient(String clientId) {
        String blockKey = RateLimitConstants.BLOCK_KEY_PREFIX + clientId;
        redisTemplate.opsForValue().set(blockKey, System.currentTimeMillis(), 
                RateLimitConstants.PENALTY_DURATION_SECONDS, TimeUnit.SECONDS);
        
        // Reset the rate limit counter
        resetLimit(clientId);
    }

    @Override
    public int getRemainingRequests(String clientId, int limit) {
        long startNs = System.nanoTime();
        String key = RateLimitConstants.RATE_LIMIT_KEY_PREFIX + clientId;
        long redisStartNs = System.nanoTime();
        Object countObj = redisTemplate.opsForValue().get(key);
        long redisGetMs = (System.nanoTime() - redisStartNs) / 1_000_000L;
        
        if (countObj == null) {
            log.info("[PERF][Service:Redis][RateLimitService] getRemainingRequests client={} | total={}ms (redisGet={}ms)",
                    clientId, (System.nanoTime() - startNs) / 1_000_000L, redisGetMs);
            return limit;
        }

        int currentCount;
        if (countObj instanceof Number) {
            currentCount = ((Number) countObj).intValue();
        } else {
            currentCount = Integer.parseInt(countObj.toString());
        }

        int remaining = Math.max(0, limit - currentCount);
        log.info("[PERF][Service:Redis][RateLimitService] getRemainingRequests client={} remaining={} | total={}ms (redisGet={}ms)",
                clientId, remaining, (System.nanoTime() - startNs) / 1_000_000L, redisGetMs);
        return remaining;
    }

    @Override
    public long getBlockTimeRemaining(String clientId) {
        String blockKey = RateLimitConstants.BLOCK_KEY_PREFIX + clientId;
        Long ttl = redisTemplate.getExpire(blockKey, TimeUnit.SECONDS);
        return ttl != null && ttl > 0 ? ttl : 0;
    }

    @Override
    public void resetLimit(String clientId) {
        String key = RateLimitConstants.RATE_LIMIT_KEY_PREFIX + clientId;
        redisTemplate.delete(key);
    }
}
