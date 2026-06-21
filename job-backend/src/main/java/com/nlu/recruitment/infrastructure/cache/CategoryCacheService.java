package com.nlu.recruitment.infrastructure.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nlu.recruitment.domain.repository.JobRepository;
import com.nlu.recruitment.api.dto.AddressJobCount;
import com.nlu.recruitment.api.dto.JobCardView;
import com.nlu.shared.domain.model.EntityStatus;
import com.nlu.recruitment.application.JobQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryCacheService {
    private static final String HOME_INIT_KEY = "cache:home:init";
    private static final String CATEGORY_KEY = "cache:home:category";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private static final TypeReference<List<JobCardView>> HOME_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<AddressJobCount>> CATEGORY_TYPE = new TypeReference<>() {
    };

    private final RedisTemplate<String, Object> redisTemplate;
    private final JobRepository jobRepository;
    private final JobQueryService jobQueryService;
    private final ObjectMapper objectMapper;

    @EventListener(ApplicationReadyEvent.class)
    public void warmUpCache() {
        preload("home:init", this::refreshHomeInitCache);
        preload("home:category", this::refreshCategoryCache);
    }

    public List<JobCardView> getHomeInitData() {
        List<JobCardView> cached = getCacheValue(HOME_INIT_KEY, HOME_TYPE);
        if (cached != null) {
            return cached;
        }
        return refreshHomeInitCache();
    }

    public List<AddressJobCount> getCategoryData() {
        List<AddressJobCount> cached = getCacheValue(CATEGORY_KEY, CATEGORY_TYPE);
        if (cached != null) {
            return cached;
        }
        return refreshCategoryCache();
    }

    private List<JobCardView> refreshHomeInitCache() {
        List<JobCardView> latest = jobRepository.findJobs(
                LocalDateTime.now(),
                EntityStatus.ACTIVE,
                PageRequest.of(0, 5, Sort.by("createdAt").descending())
        );
        saveCacheValue(HOME_INIT_KEY, latest);
        return latest;
    }

    private List<AddressJobCount> refreshCategoryCache() {
        List<AddressJobCount> latest = jobQueryService.getAddressJobCount();
        saveCacheValue(CATEGORY_KEY, latest);
        return latest;
    }

    private void preload(String cacheName, Runnable loader) {
        try {
            loader.run();
            log.info("Preloaded cache: {}", cacheName);
        } catch (Exception ex) {
            log.warn("Failed to preload cache: {}", cacheName, ex);
        }
    }

    private void saveCacheValue(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value, CACHE_TTL);
        } catch (Exception ex) {
            log.warn("Failed to save cache key: {}", key, ex);
        }
    }

    private <T> T getCacheValue(String key, TypeReference<T> typeReference) {
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached == null) {
                return null;
            }
            return objectMapper.convertValue(cached, typeReference);
        } catch (IllegalArgumentException ex) {
            log.warn("Cannot deserialize cache key: {}", key, ex);
            try {
                redisTemplate.delete(key);
            } catch (Exception deleteEx) {
                log.warn("Failed to clear invalid cache key: {}", key, deleteEx);
            }
            return null;
        } catch (Exception ex) {
            log.warn("Failed to read cache key: {}", key, ex);
            return null;
        }
    }
}
