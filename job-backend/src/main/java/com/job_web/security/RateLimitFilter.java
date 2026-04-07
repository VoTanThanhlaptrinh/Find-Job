package com.job_web.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.job_web.constant.RateLimitConstants;
import com.job_web.dto.common.ApiResponse;
import com.job_web.service.security.RateLimitService;
import com.job_web.utills.MessageUtils;
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

    private static final String X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String X_REAL_IP = "X-Real-IP";
    private static final String CF_CONNECTING_IP = "CF-Connecting-IP"; // Cloudflare
    private static final String TRUE_CLIENT_IP = "True-Client-IP";     // Cloudflare Enterprise

    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String clientIp = extractClientIp(request);
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
     * Extract client IP from request headers.
     * Checks multiple headers for proxy/CDN scenarios.
     */
    private String extractClientIp(HttpServletRequest request) {
        // Priority: Cloudflare > X-Forwarded-For > X-Real-IP > Remote Address
        
        // Cloudflare headers
        String ip = request.getHeader(CF_CONNECTING_IP);
        if (isValidIp(ip)) {
            return sanitizeIp(ip);
        }

        ip = request.getHeader(TRUE_CLIENT_IP);
        if (isValidIp(ip)) {
            return sanitizeIp(ip);
        }

        // Standard proxy headers
        ip = request.getHeader(X_FORWARDED_FOR);
        if (isValidIp(ip)) {
            // X-Forwarded-For can contain multiple IPs, take the first one (original client)
            String[] ips = ip.split(",");
            return sanitizeIp(ips[0].trim());
        }

        ip = request.getHeader(X_REAL_IP);
        if (isValidIp(ip)) {
            return sanitizeIp(ip);
        }

        // Fallback to remote address
        return request.getRemoteAddr();
    }

    /**
     * Validate IP address string.
     */
    private boolean isValidIp(String ip) {
        return ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip);
    }

    /**
     * Sanitize IP address to prevent injection.
     */
    private String sanitizeIp(String ip) {
        // Remove any potentially dangerous characters
        return ip.replaceAll("[^a-fA-F0-9.:]", "");
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
                HttpStatus.TOO_MANY_REQUESTS.value()
        );

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
                HttpStatus.TOO_MANY_REQUESTS.value()
        );

        response.setHeader("Retry-After", String.valueOf(remainingTime));
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
