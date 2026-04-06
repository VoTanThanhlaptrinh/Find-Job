package com.job_web.service.security.impl;

import com.job_web.constant.RateLimitConstants;
import com.job_web.service.security.RateLimitService;
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
    public boolean isAllowed(String clientId, int limit) {
        if (isBlocked(clientId)) {
            return false;
        }

        String key = RateLimitConstants.RATE_LIMIT_KEY_PREFIX + clientId;
        Long currentCount = redisTemplate.opsForValue().increment(key);

        if (currentCount == null) {
            return false;
        }

        // Set expiry on first request
        if (currentCount == 1) {
            redisTemplate.expire(key, RateLimitConstants.WINDOW_SIZE_SECONDS, TimeUnit.SECONDS);
        }

        // Check if limit exceeded
        if (currentCount > limit) {
            blockClient(clientId);
            log.warn("Rate limit exceeded for client: {}. Blocked for {} minutes.", 
                    clientId, RateLimitConstants.PENALTY_DURATION_SECONDS / 60);
            return false;
        }

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
        String key = RateLimitConstants.RATE_LIMIT_KEY_PREFIX + clientId;
        Object countObj = redisTemplate.opsForValue().get(key);
        
        if (countObj == null) {
            return limit;
        }

        int currentCount;
        if (countObj instanceof Number) {
            currentCount = ((Number) countObj).intValue();
        } else {
            currentCount = Integer.parseInt(countObj.toString());
        }

        return Math.max(0, limit - currentCount);
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
