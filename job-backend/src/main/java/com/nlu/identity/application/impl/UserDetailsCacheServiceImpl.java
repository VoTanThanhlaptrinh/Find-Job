package com.nlu.identity.application.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nlu.identity.application.UserDetailsCacheService;
import com.nlu.identity.domain.model.User;
import com.nlu.identity.infrastructure.config.UserDetailsCacheProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;

/**
 * Caches {@link User} principals in Redis (without password hashes) for JWT request
 * authentication and supports immediate eviction when user state mutates.
 */
@Slf4j
@Service
public class UserDetailsCacheServiceImpl implements UserDetailsCacheService {

    private static final String KEY_PREFIX = "auth:user:";

    private final RedisTemplate<String, byte[]> cacheByteRedisTemplate;
    private final UserDetailsService userDetailsService;
    private final UserDetailsCacheProperties properties;
    private final ObjectMapper objectMapper;

    public UserDetailsCacheServiceImpl(
            @Qualifier("cacheByteRedisTemplate") RedisTemplate<String, byte[]> cacheByteRedisTemplate,
            UserDetailsService userDetailsService,
            UserDetailsCacheProperties properties,
            ObjectMapper objectMapper) {
        this.cacheByteRedisTemplate = cacheByteRedisTemplate;
        this.userDetailsService = userDetailsService;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        if (!properties.isEnabled() || email == null || email.isBlank()) {
            return userDetailsService.loadUserByUsername(email);
        }

        String redisKey = buildKey(email);
        byte[] cachedBytes = cacheByteRedisTemplate.opsForValue().get(redisKey);
        if (cachedBytes != null) {
            CachedUserDetails cached = deserialize(cachedBytes);
            log.debug("[PERF][Cache:Redis][UserDetailsCache] HIT email={}", email);
            return cached.toDetachedUser();
        }

        UserDetails loaded = userDetailsService.loadUserByUsername(email);
        if (loaded instanceof User user) {
            byte[] serialized = serialize(CachedUserDetails.fromUser(user));
            cacheByteRedisTemplate.opsForValue().set(redisKey, serialized, Duration.ofSeconds(properties.getTtlSeconds()));
        }
        return loaded;
    }

    @Override
    public void evictByEmail(String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        cacheByteRedisTemplate.delete(buildKey(email));
    }

    private String buildKey(String email) {
        return KEY_PREFIX + email.trim().toLowerCase();
    }

    private byte[] serialize(CachedUserDetails dto) {
        try {
            return objectMapper.writeValueAsBytes(dto);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize cached UserDetails", e);
        }
    }

    private CachedUserDetails deserialize(byte[] bytes) {
        try {
            return objectMapper.readValue(bytes, CachedUserDetails.class);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to deserialize cached UserDetails", e);
        }
    }
}

