package com.nlu.identity.infrastructure.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nlu.identity.domain.vo.RateLimitConstants;
import com.nlu.shared.domain.model.ApiResponse;
import com.nlu.identity.application.RateLimitService;
import com.nlu.shared.utils.IpUtils;
import com.nlu.shared.utils.MessageUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter for rate limiting API requests.
 * Uses X-Forwarded-For header to identify clients behind proxies.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String clientIp = IpUtils.getClientIp(request);
        String requestUri = request.getRequestURI();

        // Skip rate limiting for static resources
        if (isStaticResource(requestUri)) {
            filterChain.doFilter(request, response);
            return;
        }

        long selfStartNs = System.nanoTime();

        // Check if client is blocked
        long blockedStartNs = System.nanoTime();
        boolean blocked = rateLimitService.isBlocked(clientIp);
        long isBlockedMs = (System.nanoTime() - blockedStartNs) / 1_000_000L;
        if (blocked) {
            long remainingTime = rateLimitService.getBlockTimeRemaining(clientIp);
            sendBlockedResponse(response, remainingTime);
            log.info("[PERF][Filter:RateLimit] {} {} | blocked=true selfRedisTotal={}ms (isBlocked={}ms)",
                    request.getMethod(), requestUri, (System.nanoTime() - selfStartNs) / 1_000_000L, isBlockedMs);
            return;
        }

        // Determine rate limit based on authentication status
        int rateLimit = determineRateLimit();

        // Check rate limit and compute remaining requests in a single flow
        long consumeStartNs = System.nanoTime();
        int remaining = rateLimitService.consumeAndGetRemaining(clientIp, rateLimit);
        long consumeMs = (System.nanoTime() - consumeStartNs) / 1_000_000L;
        if (remaining < 0) {
            long remainingTime = rateLimitService.getBlockTimeRemaining(clientIp);
            sendRateLimitExceededResponse(response, remainingTime);
            log.info("[PERF][Filter:RateLimit] {} {} | rateLimited=true selfRedisTotal={}ms (isBlocked={}ms, consume={}ms)",
                    request.getMethod(), requestUri, (System.nanoTime() - selfStartNs) / 1_000_000L, isBlockedMs, consumeMs);
            return;
        }

        // Add rate limit headers without extra Redis GET
        addRateLimitHeaders(response, rateLimit, remaining);
        long selfRedisTotalMs = (System.nanoTime() - selfStartNs) / 1_000_000L;

        long downstreamStartNs = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long downstreamMs = (System.nanoTime() - downstreamStartNs) / 1_000_000L;
            log.info("[PERF][Filter:RateLimit] {} {} | selfRedisTotal={}ms (isBlocked={}ms, consume={}ms) | downstreamChain={}ms",
                    request.getMethod(), requestUri, selfRedisTotalMs, isBlockedMs, consumeMs, downstreamMs);
        }
    }

    /**
     * Determine rate limit based on authentication status.
     */
    private int determineRateLimit() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            return RateLimitConstants.AUTHENTICATED_RATE_LIMIT;
        }

        return RateLimitConstants.PUBLIC_RATE_LIMIT;
    }

    /**
     * Check if the request is for static resources.
     */
    private boolean isStaticResource(String uri) {
        return uri.startsWith("/static/")
                || uri.startsWith("/assets/")
                || uri.startsWith("/favicon")
                || uri.endsWith(".css")
                || uri.endsWith(".js")
                || uri.endsWith(".png")
                || uri.endsWith(".jpg")
                || uri.endsWith(".ico");
    }

    /**
     * Add rate limit information headers to response.
     */
    private void addRateLimitHeaders(HttpServletResponse response, int limit, int remaining) {
        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
        response.setHeader("X-RateLimit-Reset", String.valueOf(RateLimitConstants.WINDOW_SIZE_SECONDS));
    }

    /**
     * Send blocked response when client is in penalty period.
     */
    private void sendBlockedResponse(HttpServletResponse response, long remainingTime) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        String message = MessageUtils.getMessage("rate_limit.blocked", remainingTime / 60, remainingTime % 60);

        ApiResponse<Object> apiResponse = new ApiResponse<>(
                message,
                null,
                HttpStatus.TOO_MANY_REQUESTS.value());

        response.setHeader("Retry-After", String.valueOf(remainingTime));
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }

    /**
     * Send rate limit exceeded response.
     */
    private void sendRateLimitExceededResponse(HttpServletResponse response, long remainingTime) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        String message = MessageUtils.getMessage("rate_limit.exceeded", remainingTime / 60);

        ApiResponse<Object> apiResponse = new ApiResponse<>(
                message,
                null,
                HttpStatus.TOO_MANY_REQUESTS.value());

        response.setHeader("Retry-After", String.valueOf(remainingTime));
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
