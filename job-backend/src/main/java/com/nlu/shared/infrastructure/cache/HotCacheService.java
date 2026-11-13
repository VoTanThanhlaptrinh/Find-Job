package com.nlu.shared.infrastructure.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Manages Redis hot-GET counting, ZSET quota enforcement, binary/Gzip response storage,
 * single-flight stampede locking, and targeted/group cache invalidation.
 */
@Slf4j
@Service
public class HotCacheService {

    private final RedisTemplate<String, byte[]> cacheByteRedisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final HotCacheProperties properties;
    private final HotCachePayloadCodec payloadCodec;
    private final HotCacheKeyBuilder keyBuilder;
    private final ApplicationEventPublisher eventPublisher;

    public HotCacheService(
            @Qualifier("cacheByteRedisTemplate") RedisTemplate<String, byte[]> cacheByteRedisTemplate,
            StringRedisTemplate stringRedisTemplate,
            HotCacheProperties properties,
            HotCachePayloadCodec payloadCodec,
            HotCacheKeyBuilder keyBuilder,
            ApplicationEventPublisher eventPublisher) {
        this.cacheByteRedisTemplate = cacheByteRedisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
        this.properties = properties;
        this.payloadCodec = payloadCodec;
        this.keyBuilder = keyBuilder;
        this.eventPublisher = eventPublisher;
    }

    public Optional<CachedHttpResponse> getCachedResponse(String entryKey) {
        byte[] raw = cacheByteRedisTemplate.opsForValue().get(entryKey);
        if (raw == null) {
            return Optional.empty();
        }
        return Optional.of(payloadCodec.decode(raw));
    }

    public long incrementAndGetCounter(String counterKey, long windowSeconds) {
        Long count = stringRedisTemplate.opsForValue().increment(counterKey);
        if (count == null) {
            throw new IllegalStateException("Redis increment returned null for counter key: " + counterKey);
        }
        if (count == 1L) {
            stringRedisTemplate.expire(counterKey, windowSeconds, TimeUnit.SECONDS);
        }
        return count;
    }

    public boolean tryAcquireSingleFlightLock(String lockKey, long lockTtlSeconds) {
        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", Duration.ofSeconds(lockTtlSeconds));
        return Boolean.TRUE.equals(acquired);
    }

    public void releaseSingleFlightLock(String lockKey) {
        stringRedisTemplate.delete(lockKey);
    }

    public void storeResponseWithQuota(
            HotCacheKeyContext keyContext,
            CachedHttpResponse response) {

        HotCacheProperties.Rule rule = keyContext.rule();
        long ttlSeconds = rule.getTtlSeconds() > 0 ? rule.getTtlSeconds() : HotCacheConstants.DEFAULT_TTL_SECONDS;
        int maxEntries = rule.getMaxEntries() > 0 ? rule.getMaxEntries() : HotCacheConstants.DEFAULT_MAX_ENTRIES;

        byte[] encoded = payloadCodec.encode(response, properties.getGzipThresholdBytes());
        cacheByteRedisTemplate.opsForValue().set(keyContext.entryKey(), encoded, Duration.ofSeconds(ttlSeconds));

        long nowMs = System.currentTimeMillis();
        long expireScore = nowMs + (ttlSeconds * 1_000L);
        String indexKey = keyContext.indexKey();

        stringRedisTemplate.opsForZSet().removeRangeByScore(indexKey, 0, nowMs);
        stringRedisTemplate.opsForZSet().add(indexKey, keyContext.entryKey(), expireScore);
        stringRedisTemplate.expire(indexKey, ttlSeconds, TimeUnit.SECONDS);

        enforceQuota(indexKey, maxEntries);
    }

    public void invalidateForWrite(String method, String uri, Authentication authentication) {
        if (!properties.isEnabled() || method == null || uri == null) {
            return;
        }
        for (HotCacheProperties.Rule rule : properties.getRules()) {
            List<String> triggers = rule.getInvalidateOn();
            if (triggers == null || triggers.isEmpty()) {
                continue;
            }
            for (String trigger : triggers) {
                if (keyBuilder.matchesPattern(trigger, method, uri)) {
                    invalidateMatchedRule(rule, trigger, uri, authentication);
                    break;
                }
            }
        }
    }

    public void evictRule(String ruleId) {
        String indexKey = HotCacheConstants.KEY_PREFIX_INDEX + ruleId;
        Set<String> members = stringRedisTemplate.opsForZSet().range(indexKey, 0, -1);
        if (members != null && !members.isEmpty()) {
            cacheByteRedisTemplate.delete(members);
        }
        stringRedisTemplate.delete(indexKey);
        eventPublisher.publishEvent(new HotCacheInvalidatedEvent(ruleId));
    }

    public void evictUserRuleEntries(String ruleId, long userId, String resourceFragment) {
        String indexKey = HotCacheConstants.KEY_PREFIX_INDEX + ruleId;
        Set<String> members = stringRedisTemplate.opsForZSet().range(indexKey, 0, -1);
        if (members == null || members.isEmpty()) {
            return;
        }
        String userSegment = ":user_" + userId + ":";
        List<String> toDelete = new ArrayList<>();
        for (String member : members) {
            if (!member.contains(userSegment)) {
                continue;
            }
            if (resourceFragment != null && !member.contains(resourceFragment)) {
                continue;
            }
            toDelete.add(member);
        }
        deleteEntriesFromIndex(indexKey, toDelete);
        eventPublisher.publishEvent(new HotCacheInvalidatedEvent(ruleId));
    }

    private void enforceQuota(String indexKey, int maxEntries) {
        Long currentSize = stringRedisTemplate.opsForZSet().zCard(indexKey);
        if (currentSize == null || currentSize <= maxEntries) {
            return;
        }
        long excess = currentSize - maxEntries;
        Set<ZSetOperations.TypedTuple<String>> popped = stringRedisTemplate.opsForZSet().popMin(indexKey, excess);
        if (popped == null || popped.isEmpty()) {
            return;
        }
        List<String> keysToDelete = popped.stream()
                .map(ZSetOperations.TypedTuple::getValue)
                .filter(v -> v != null && !v.isBlank())
                .toList();
        if (!keysToDelete.isEmpty()) {
            cacheByteRedisTemplate.delete(keysToDelete);
        }
    }

    private void invalidateMatchedRule(
            HotCacheProperties.Rule rule,
            String triggerPattern,
            String writeUri,
            Authentication authentication) {

        Map<String, String> writeVars = keyBuilder.extractTemplateVariables(triggerPattern, writeUri);
        String resourceMarker = resolveMatchingResourceMarker(rule.getPattern(), writeVars);
        boolean forceAllUsers = triggerPattern.contains("[all]");

        Optional<String> userScopeFilter = Optional.empty();
        if (rule.getScope() == HotCacheScope.USER && !forceAllUsers) {
            userScopeFilter = keyBuilder.resolveUserScope(authentication);
        }

        if (resourceMarker == null && userScopeFilter.isEmpty()) {
            evictRule(rule.getId());
            return;
        }

        String indexKey = HotCacheConstants.KEY_PREFIX_INDEX + rule.getId();
        Set<String> members = stringRedisTemplate.opsForZSet().range(indexKey, 0, -1);
        if (members == null || members.isEmpty()) {
            eventPublisher.publishEvent(new HotCacheInvalidatedEvent(rule.getId()));
            return;
        }

        List<String> toDelete = new ArrayList<>();
        for (String member : members) {
            if (userScopeFilter.isPresent() && !member.contains(":" + userScopeFilter.get() + ":")) {
                continue;
            }
            if (resourceMarker != null && !member.contains(resourceMarker)) {
                continue;
            }
            toDelete.add(member);
        }
        deleteEntriesFromIndex(indexKey, toDelete);
        eventPublisher.publishEvent(new HotCacheInvalidatedEvent(rule.getId()));
    }

    private String resolveMatchingResourceMarker(String getPattern, Map<String, String> writeVars) {
        if (writeVars.isEmpty()) {
            return null;
        }
        for (Map.Entry<String, String> entry : writeVars.entrySet()) {
            String key = entry.getKey();
            if (getPattern != null && getPattern.contains("{" + key + "}")) {
                return "[" + key + "=" + entry.getValue() + "]";
            }
        }
        return null;
    }

    private void deleteEntriesFromIndex(String indexKey, List<String> toDelete) {
        if (toDelete.isEmpty()) {
            return;
        }
        cacheByteRedisTemplate.delete(toDelete);
        stringRedisTemplate.opsForZSet().remove(indexKey, toDelete.toArray());
    }
}
