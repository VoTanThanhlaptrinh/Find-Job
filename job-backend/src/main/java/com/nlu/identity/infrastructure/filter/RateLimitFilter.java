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

        // Check if client is blocked
        if (rateLimitService.isBlocked(clientIp)) {
            long remainingTime = rateLimitService.getBlockTimeRemaining(clientIp);
            sendBlockedResponse(response, remainingTime);
            return;
        }

        // Determine rate limit based on authentication status
        int rateLimit = determineRateLimit();

        // Check rate limit
        if (!rateLimitService.isAllowed(clientIp, rateLimit)) {
            long remainingTime = rateLimitService.getBlockTimeRemaining(clientIp);
            sendRateLimitExceededResponse(response, remainingTime);
            return;
        }

        // Add rate limit headers
        addRateLimitHeaders(response, clientIp, rateLimit);

        filterChain.doFilter(request, response);
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
    private void addRateLimitHeaders(HttpServletResponse response, String clientIp, int limit) {
        int remaining = rateLimitService.getRemainingRequests(clientIp, limit);
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
