package com.nlu.shared.infrastructure.cache;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Protected HTTP GET cache and write-invalidation filter registered AFTER {@code AuthorizationFilter}.
 * Handles {@link HotCacheScope#ROLE} and {@link HotCacheScope#USER} GET caching as well as
 * automatic cache eviction when mutating requests succeed with 2xx.
 */
@RequiredArgsConstructor
public class ProtectedHotGetCacheFilter extends OncePerRequestFilter {

    private final HotGetCacheSupport cacheSupport;
    private final HotCacheService hotCacheService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String method = request.getMethod() != null ? request.getMethod().toUpperCase() : "";
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if ("GET".equals(method)) {
            cacheSupport.processGetWithCache(
                    request,
                    response,
                    filterChain,
                    rule -> rule.getScope() != HotCacheScope.PUBLIC,
                    authentication);
            return;
        }

        filterChain.doFilter(request, response);

        if (HotCacheConstants.MUTATING_METHODS.contains(method)
                && response.getStatus() >= 200
                && response.getStatus() < 300) {
            hotCacheService.invalidateForWrite(method, request.getRequestURI(), authentication);
        }
    }
}

