package com.job_web.service.security.impl;

import java.time.Duration;

import com.job_web.service.security.JwtFamilyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtFamilyServiceImpl implements JwtFamilyService {
    private static final String KEY_PREFIX = "auth:family:";
    private static final Duration TTL = Duration.ofDays(7);

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void saveFamilyJti(String familyId, String jti) {
        redisTemplate.opsForValue().set(buildKey(familyId), jti, TTL);
    }

    @Override
    public void deleteFamily(String familyId) {
        redisTemplate.delete(buildKey(familyId));
    }

    @Override
    public String getFamilyJti(String familyId) {
        Object value = redisTemplate.opsForValue().get(buildKey(familyId));
        return value != null ? value.toString() : null;
    }

    private String buildKey(String familyId) {
        return KEY_PREFIX + familyId;
    }
}
