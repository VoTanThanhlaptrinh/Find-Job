package com.nlu.shared.infrastructure.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final IdempotencyProperties properties;
    private final ObjectMapper objectMapper;

    public boolean tryAcquire(String scope, String idempotencyKey, String requestHash, String lockToken) {
        long startNs = System.nanoTime();
        String redisKey = buildRedisKey(scope, idempotencyKey);
        IdempotencyRecord inProgress = IdempotencyRecord.builder()
                .status(IdempotencyStatus.IN_PROGRESS)
                .requestHash(requestHash)
                .lockToken(lockToken)
                .createdAtEpochMs(System.currentTimeMillis())
                .build();
        long javaBuildMs = toMillis(System.nanoTime() - startNs);

        Duration ttl = Duration.ofSeconds(properties.getLockTtlSeconds());
        long redisStartNs = System.nanoTime();
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(redisKey, inProgress, ttl);
        long redisSetIfAbsentMs = toMillis(System.nanoTime() - redisStartNs);
        long totalMs = toMillis(System.nanoTime() - startNs);
        log.info("[PERF][Service:Redis][IdempotencyService] tryAcquire key={} acquired={} | total={}ms (javaBuild={}ms, redisSetIfAbsent={}ms)",
                redisKey, Boolean.TRUE.equals(acquired), totalMs, javaBuildMs, redisSetIfAbsentMs);
        return Boolean.TRUE.equals(acquired);
    }

    public Optional<IdempotencyRecord> getRecord(String scope, String idempotencyKey) {
        long startNs = System.nanoTime();
        String redisKey = buildRedisKey(scope, idempotencyKey);
        long redisStartNs = System.nanoTime();
        Object raw = redisTemplate.opsForValue().get(redisKey);
        long redisGetMs = toMillis(System.nanoTime() - redisStartNs);
        if (raw == null) {
            log.info("[PERF][Service:Redis][IdempotencyService] getRecord key={} hit=false | total={}ms (redisGet={}ms, javaConvert=0ms)",
                    redisKey, toMillis(System.nanoTime() - startNs), redisGetMs);
            return Optional.empty();
        }
        if (raw instanceof IdempotencyRecord record) {
            log.info("[PERF][Service:Redis][IdempotencyService] getRecord key={} hit=true | total={}ms (redisGet={}ms, javaConvert=0ms)",
                    redisKey, toMillis(System.nanoTime() - startNs), redisGetMs);
            return Optional.of(record);
        }
        long convertStartNs = System.nanoTime();
        IdempotencyRecord converted = objectMapper.convertValue(raw, IdempotencyRecord.class);
        long javaConvertMs = toMillis(System.nanoTime() - convertStartNs);
        log.info("[PERF][Service:Redis][IdempotencyService] getRecord key={} hit=true | total={}ms (redisGet={}ms, javaConvert={}ms)",
                redisKey, toMillis(System.nanoTime() - startNs), redisGetMs, javaConvertMs);
        return Optional.of(converted);
    }

    public void markCompleted(
            String scope,
            String idempotencyKey,
            String requestHash,
            String lockToken,
            int httpStatus,
            String contentType,
            String responseBody) {
        long startNs = System.nanoTime();
        String redisKey = buildRedisKey(scope, idempotencyKey);
        IdempotencyRecord completed = IdempotencyRecord.builder()
                .status(IdempotencyStatus.COMPLETED)
                .requestHash(requestHash)
                .lockToken(lockToken)
                .httpStatus(httpStatus)
                .contentType(contentType)
                .responseBody(responseBody)
                .createdAtEpochMs(System.currentTimeMillis())
                .build();
        long javaBuildMs = toMillis(System.nanoTime() - startNs);

        Duration ttl = Duration.ofSeconds(properties.getCompletedTtlSeconds());
        long redisStartNs = System.nanoTime();
        redisTemplate.opsForValue().set(redisKey, completed, ttl);
        long redisSetMs = toMillis(System.nanoTime() - redisStartNs);
        log.info("[PERF][Service:Redis][IdempotencyService] markCompleted key={} | total={}ms (javaBuild={}ms, redisSet={}ms)",
                redisKey, toMillis(System.nanoTime() - startNs), javaBuildMs, redisSetMs);
    }

    public void releaseLock(String scope, String idempotencyKey, String lockToken) {
        if (lockToken == null) {
            return;
        }
        long startNs = System.nanoTime();
        Optional<IdempotencyRecord> existingOpt = getRecord(scope, idempotencyKey);
        if (existingOpt.isEmpty()) {
            log.info("[PERF][Service:Redis][IdempotencyService] releaseLock key={} deleted=false (missing) | total={}ms",
                    buildRedisKey(scope, idempotencyKey), toMillis(System.nanoTime() - startNs));
            return;
        }
        IdempotencyRecord existing = existingOpt.get();
        if (existing.getStatus() != IdempotencyStatus.IN_PROGRESS) {
            log.info("[PERF][Service:Redis][IdempotencyService] releaseLock key={} deleted=false (status={}) | total={}ms",
                    buildRedisKey(scope, idempotencyKey), existing.getStatus(), toMillis(System.nanoTime() - startNs));
            return;
        }
        if (!Objects.equals(existing.getLockToken(), lockToken)) {
            log.info("[PERF][Service:Redis][IdempotencyService] releaseLock key={} deleted=false (tokenMismatch) | total={}ms",
                    buildRedisKey(scope, idempotencyKey), toMillis(System.nanoTime() - startNs));
            return;
        }
        long deleteStartNs = System.nanoTime();
        redisTemplate.delete(buildRedisKey(scope, idempotencyKey));
        long redisDeleteMs = toMillis(System.nanoTime() - deleteStartNs);
        log.info("[PERF][Service:Redis][IdempotencyService] releaseLock key={} deleted=true | total={}ms (redisDelete={}ms)",
                buildRedisKey(scope, idempotencyKey), toMillis(System.nanoTime() - startNs), redisDeleteMs);
    }

    private long toMillis(long elapsedNs) {
        return elapsedNs / 1_000_000L;
    }

    public String buildRedisKey(String scope, String idempotencyKey) {
        return IdempotencyConstants.REDIS_KEY_PREFIX + scope + ":" + idempotencyKey;
    }
}

