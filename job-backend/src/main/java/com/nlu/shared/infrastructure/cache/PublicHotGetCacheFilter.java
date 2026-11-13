package com.nlu.shared.infrastructure.cache;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Fast-path HTTP GET cache filter for {@link HotCacheScope#PUBLIC} endpoints.
 * Registered before {@code JwtFilter} so public cache hits bypass security context and DB overhead.
 */
@RequiredArgsConstructor
public class PublicHotGetCacheFilter extends OncePerRequestFilter {

    private final HotGetCacheSupport cacheSupport;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        cacheSupport.processGetWithCache(
                request,
                response,
                filterChain,
                rule -> rule.getScope() == HotCacheScope.PUBLIC,
                null);
    }
}

