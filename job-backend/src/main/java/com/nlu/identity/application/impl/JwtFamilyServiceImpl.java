package com.nlu.identity.application.impl;

import java.time.Duration;
import java.util.Set;

import com.nlu.identity.application.JwtFamilyService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtFamilyServiceImpl implements JwtFamilyService {
    private static final String KEY_PREFIX = "auth:family:";
    private static final String USER_FAMILIES_PREFIX = "auth:user-families:";

    private final RedisTemplate<String, Object> redisTemplate;
    @Value("${application.service.impl.timeLimit}")
    private long refreshTokenTtlSeconds;

    @Override
    public void saveFamilyJti(String familyId, String jti) {
        redisTemplate.opsForValue().set(buildKey(familyId), jti, getTtl());
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

    @Override
    public void saveUserFamily(String username, String familyId) {
        if (username == null || familyId == null) {
            return;
        }
        String userKey = buildUserFamiliesKey(username);
        redisTemplate.opsForSet().add(userKey, familyId);
        redisTemplate.expire(userKey, getTtl());
    }

    private Duration getTtl() {
        return Duration.ofSeconds(refreshTokenTtlSeconds);
    }

    @Override
    public void revokeAllUserFamilies(String username) {
        if (username == null) {
            return;
        }
        String userKey = buildUserFamiliesKey(username);
        Set<Object> families = redisTemplate.opsForSet().members(userKey);
        if (families != null) {
            for (Object familyId : families) {
                if (familyId != null) {
                    deleteFamily(familyId.toString());
                }
            }
        }
        redisTemplate.delete(userKey);
    }

    private String buildKey(String familyId) {
        return KEY_PREFIX + familyId;
    }

    private String buildUserFamiliesKey(String username) {
        return USER_FAMILIES_PREFIX + username;
    }
}
