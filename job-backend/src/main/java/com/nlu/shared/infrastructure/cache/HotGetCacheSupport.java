package com.nlu.shared.infrastructure.cache;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Shared execution engine for {@link PublicHotGetCacheFilter} and {@link ProtectedHotGetCacheFilter}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HotGetCacheSupport {

    private final HotCacheProperties properties;
    private final HotCacheKeyBuilder keyBuilder;
    private final HotCacheService hotCacheService;

    public void processGetWithCache(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain,
            Predicate<HotCacheProperties.Rule> scopePredicate,
            Authentication authentication) throws ServletException, IOException {

        if (!properties.isEnabled() || !"GET".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<HotCacheProperties.Rule> ruleOpt = keyBuilder.findMatchingRule(request);
        if (ruleOpt.isEmpty() || !scopePredicate.test(ruleOpt.get())) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<HotCacheKeyContext> contextOpt = keyBuilder.buildKeyContext(request, ruleOpt.get(), authentication);
        if (contextOpt.isEmpty()) {
            response.setHeader(HotCacheConstants.HEADER_CACHE_STATUS, HotCacheConstants.CACHE_BYPASS);
            filterChain.doFilter(request, response);
            return;
        }

        HotCacheKeyContext keyContext = contextOpt.get();
        Optional<CachedHttpResponse> cachedOpt = hotCacheService.getCachedResponse(keyContext.entryKey());
        if (cachedOpt.isPresent()) {
            writeCachedResponse(response, cachedOpt.get(), HotCacheConstants.CACHE_HIT);
            return;
        }

        HotCacheProperties.Rule rule = keyContext.rule();
        int threshold = rule.getThreshold() != null ? rule.getThreshold() : properties.getDefaultThreshold();
        long windowSeconds = rule.getWindowSeconds() != null ? rule.getWindowSeconds() : properties.getDefaultWindowSeconds();

        long currentCount = hotCacheService.incrementAndGetCounter(keyContext.counterKey(), windowSeconds);
        if (currentCount < threshold) {
            response.setHeader(HotCacheConstants.HEADER_CACHE_STATUS, HotCacheConstants.CACHE_COUNTING);
            filterChain.doFilter(request, response);
            return;
        }

        handleHotCacheMissWithSingleFlight(request, response, filterChain, keyContext);
    }

    private void handleHotCacheMissWithSingleFlight(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain,
            HotCacheKeyContext keyContext) throws ServletException, IOException {

        boolean lockAcquired = hotCacheService.tryAcquireSingleFlightLock(
                keyContext.lockKey(),
                properties.getLockTtlSeconds());

        if (!lockAcquired) {
            Optional<CachedHttpResponse> awaited = waitForSingleFlightCache(keyContext.entryKey());
            if (awaited.isPresent()) {
                writeCachedResponse(response, awaited.get(), HotCacheConstants.CACHE_HIT);
                return;
            }
            response.setHeader(HotCacheConstants.HEADER_CACHE_STATUS, HotCacheConstants.CACHE_BYPASS);
            filterChain.doFilter(request, response);
            return;
        }

        boolean createdWrapper = !(response instanceof ContentCachingResponseWrapper);
        ContentCachingResponseWrapper cachingResponse = createdWrapper
                ? new ContentCachingResponseWrapper(response)
                : (ContentCachingResponseWrapper) response;

        try {
            filterChain.doFilter(request, cachingResponse);
            storeIfCacheable(request, cachingResponse, keyContext);
        } finally {
            hotCacheService.releaseSingleFlightLock(keyContext.lockKey());
            if (createdWrapper) {
                cachingResponse.copyBodyToResponse();
            }
        }
    }

    private Optional<CachedHttpResponse> waitForSingleFlightCache(String entryKey) {
        int maxRetries = Math.max(0, properties.getSingleFlightMaxWaitRetries());
        long waitMs = Math.max(1L, properties.getSingleFlightWaitMs());

        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                Thread.sleep(waitMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return Optional.empty();
            }
            Optional<CachedHttpResponse> cached = hotCacheService.getCachedResponse(entryKey);
            if (cached.isPresent()) {
                return cached;
            }
        }
        return Optional.empty();
    }

    private void storeIfCacheable(
            HttpServletRequest request,
            ContentCachingResponseWrapper cachingResponse,
            HotCacheKeyContext keyContext) {

        if (request.isAsyncStarted()) {
            return;
        }
        int status = cachingResponse.getStatus();
        byte[] bodyBytes = cachingResponse.getContentAsByteArray();
        if (status < 200 || status >= 300 || bodyBytes.length > properties.getMaxBodyBytes()) {
            cachingResponse.setHeader(HotCacheConstants.HEADER_CACHE_STATUS, HotCacheConstants.CACHE_BYPASS);
            return;
        }

        String contentType = cachingResponse.getContentType() != null
                ? cachingResponse.getContentType()
                : MediaType.APPLICATION_JSON_VALUE;

        hotCacheService.storeResponseWithQuota(
                keyContext,
                new CachedHttpResponse(status, contentType, bodyBytes));
        cachingResponse.setHeader(HotCacheConstants.HEADER_CACHE_STATUS, HotCacheConstants.CACHE_MISS_STORED);
    }

    private void writeCachedResponse(
            HttpServletResponse response,
            CachedHttpResponse cached,
            String cacheStatusHeader) throws IOException {

        response.setStatus(cached.httpStatus());
        response.setContentType(cached.contentType() != null
                ? cached.contentType()
                : MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(HotCacheConstants.HEADER_CACHE_STATUS, cacheStatusHeader);
        byte[] body = cached.body() != null ? cached.body() : new byte[0];
        response.getOutputStream().write(body);
        response.getOutputStream().flush();
    }
}

