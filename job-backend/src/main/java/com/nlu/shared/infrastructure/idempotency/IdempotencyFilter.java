package com.nlu.shared.infrastructure.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nlu.identity.domain.model.User;
import com.nlu.shared.domain.model.ApiResponse;
import com.nlu.shared.utils.IpUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyFilter extends OncePerRequestFilter {

    private static final String MDC_TRACE_ID_KEY = "traceId";

    private final IdempotencyProperties properties;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        long filterStartNs = System.nanoTime();
        long endpointCheckStartNs = System.nanoTime();
        boolean enabledAndMatched = properties.isEnabled() && isIdempotentEndpoint(request);
        long javaEndpointCheckMs = toMillis(System.nanoTime() - endpointCheckStartNs);

        if (!enabledAndMatched) {
            filterChain.doFilter(request, response);
            return;
        }

        String rawKey = request.getHeader(properties.getHeaderName());
        if (rawKey == null || rawKey.isBlank()) {
            writeErrorResponse(response, HttpStatus.BAD_REQUEST,
                    properties.getHeaderName() + " header is required for this endpoint");
            return;
        }

        String idempotencyKey = rawKey.trim();
        if (idempotencyKey.length() > IdempotencyConstants.MAX_KEY_LENGTH) {
            writeErrorResponse(response, HttpStatus.BAD_REQUEST,
                    properties.getHeaderName() + " must not exceed " + IdempotencyConstants.MAX_KEY_LENGTH + " characters");
            return;
        }

        long wrapStartNs = System.nanoTime();
        CachedBodyHttpServletRequest cachedRequest = wrapRequest(request);
        long javaWrapBodyMs = toMillis(System.nanoTime() - wrapStartNs);

        long hashScopeStartNs = System.nanoTime();
        String requestHash = computeRequestHash(cachedRequest);
        String scope = resolvePrincipalScope(cachedRequest);
        String lockToken = UUID.randomUUID().toString();
        long javaHashAndScopeMs = toMillis(System.nanoTime() - hashScopeStartNs);

        long tryAcquireStartNs = System.nanoTime();
        boolean acquired = idempotencyService.tryAcquire(scope, idempotencyKey, requestHash, lockToken);
        long redisTryAcquireMs = toMillis(System.nanoTime() - tryAcquireStartNs);

        if (!acquired) {
            long existingStartNs = System.nanoTime();
            handleExistingIdempotencyRecord(response, scope, idempotencyKey, requestHash);
            long handleExistingMs = toMillis(System.nanoTime() - existingStartNs);
            long totalMs = toMillis(System.nanoTime() - filterStartNs);
            log.info("[PERF][Filter:Idempotency][DuplicateOrReplay] {} {} key={} | total={}ms (javaEndpointCheck={}ms, javaWrapBody={}ms, javaHashAndScope={}ms, redisTryAcquire={}ms, handleExisting={}ms)",
                    request.getMethod(), request.getRequestURI(), idempotencyKey,
                    totalMs, javaEndpointCheckMs, javaWrapBodyMs, javaHashAndScopeMs, redisTryAcquireMs, handleExistingMs);
            return;
        }

        boolean createdResponseWrapper = !(response instanceof ContentCachingResponseWrapper);
        ContentCachingResponseWrapper cachingResponse = createdResponseWrapper
                ? new ContentCachingResponseWrapper(response)
                : (ContentCachingResponseWrapper) response;

        long downstreamChainMs = 0;
        long redisFinalizeMs = 0;
        long javaCopyResponseMs = 0;

        try {
            long chainStartNs = System.nanoTime();
            filterChain.doFilter(cachedRequest, cachingResponse);
            downstreamChainMs = toMillis(System.nanoTime() - chainStartNs);

            long finalizeStartNs = System.nanoTime();
            finalizeIdempotencyExecution(cachingResponse, scope, idempotencyKey, requestHash, lockToken);
            redisFinalizeMs = toMillis(System.nanoTime() - finalizeStartNs);
        } catch (Exception ex) {
            long releaseStartNs = System.nanoTime();
            idempotencyService.releaseLock(scope, idempotencyKey, lockToken);
            redisFinalizeMs = toMillis(System.nanoTime() - releaseStartNs);
            throw ex;
        } finally {
            if (createdResponseWrapper) {
                long copyStartNs = System.nanoTime();
                cachingResponse.copyBodyToResponse();
                javaCopyResponseMs = toMillis(System.nanoTime() - copyStartNs);
            }
            long totalMs = toMillis(System.nanoTime() - filterStartNs);
            log.info("[PERF][Filter:Idempotency] {} {} key={} | total={}ms (javaEndpointCheck={}ms, javaWrapBody={}ms, javaHashAndScope={}ms, redisTryAcquire={}ms, downstreamChain={}ms, redisFinalize={}ms, javaCopyResponse={}ms)",
                    request.getMethod(), request.getRequestURI(), idempotencyKey,
                    totalMs, javaEndpointCheckMs, javaWrapBodyMs, javaHashAndScopeMs, redisTryAcquireMs,
                    downstreamChainMs, redisFinalizeMs, javaCopyResponseMs);
        }
    }

    private void handleExistingIdempotencyRecord(
            HttpServletResponse response,
            String scope,
            String idempotencyKey,
            String requestHash) throws IOException {

        Optional<IdempotencyRecord> existingOpt = idempotencyService.getRecord(scope, idempotencyKey);
        if (existingOpt.isEmpty()) {
            writeErrorResponse(response, HttpStatus.CONFLICT,
                    "A concurrent request with this Idempotency-Key is being processed. Please retry.");
            return;
        }

        IdempotencyRecord existing = existingOpt.get();
        if (!Objects.equals(existing.getRequestHash(), requestHash)) {
            writeErrorResponse(response, HttpStatus.CONFLICT,
                    "Idempotency-Key has already been used with a different request payload");
            return;
        }

        if (existing.getStatus() == IdempotencyStatus.IN_PROGRESS) {
            writeErrorResponse(response, HttpStatus.CONFLICT,
                    "A request with this Idempotency-Key is currently in progress");
            return;
        }

        replayCompletedResponse(response, existing);
    }

    private void finalizeIdempotencyExecution(
            ContentCachingResponseWrapper cachingResponse,
            String scope,
            String idempotencyKey,
            String requestHash,
            String lockToken) {

        int status = cachingResponse.getStatus();
        byte[] bodyBytes = cachingResponse.getContentAsByteArray();

        if (status < 200 || status >= 300 || bodyBytes.length > IdempotencyConstants.MAX_CACHEABLE_BODY_BYTES) {
            idempotencyService.releaseLock(scope, idempotencyKey, lockToken);
            return;
        }

        String responseBody = new String(bodyBytes, StandardCharsets.UTF_8);
        String contentType = cachingResponse.getContentType() != null
                ? cachingResponse.getContentType()
                : MediaType.APPLICATION_JSON_VALUE;

        idempotencyService.markCompleted(
                scope,
                idempotencyKey,
                requestHash,
                lockToken,
                status,
                contentType,
                responseBody);
        cachingResponse.setHeader(IdempotencyConstants.HEADER_IDEMPOTENT_REPLAYED, "false");
    }

    private void replayCompletedResponse(HttpServletResponse response, IdempotencyRecord existing) throws IOException {
        response.setStatus(existing.getHttpStatus());
        response.setContentType(existing.getContentType() != null
                ? existing.getContentType()
                : MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(IdempotencyConstants.HEADER_IDEMPOTENT_REPLAYED, "true");

        if (existing.getResponseBody() != null) {
            response.getWriter().write(existing.getResponseBody());
        }
    }

    public boolean isIdempotentEndpoint(HttpServletRequest request) {
        List<String> rules = properties.getEndpoints();
        if (rules == null || rules.isEmpty()) {
            return false;
        }

        String method = request.getMethod() != null ? request.getMethod().toUpperCase() : "";
        String uri = request.getRequestURI();

        for (String rawRule : rules) {
            if (matchesRule(rawRule, method, uri)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesRule(String rawRule, String requestMethod, String requestUri) {
        if (rawRule == null || rawRule.isBlank()) {
            return false;
        }
        String trimmed = rawRule.trim();
        int spaceIdx = trimmed.indexOf(' ');
        if (spaceIdx <= 0) {
            return IdempotencyConstants.DEFAULT_MUTATING_METHODS.contains(requestMethod)
                    && pathMatcher.match(trimmed, requestUri);
        }

        String configuredMethod = trimmed.substring(0, spaceIdx).trim().toUpperCase();
        String configuredPattern = trimmed.substring(spaceIdx + 1).trim();
        boolean methodMatches = "*".equals(configuredMethod) || configuredMethod.equals(requestMethod);
        return methodMatches && pathMatcher.match(configuredPattern, requestUri);
    }

    private CachedBodyHttpServletRequest wrapRequest(HttpServletRequest request) throws IOException {
        if (request instanceof CachedBodyHttpServletRequest cached) {
            return cached;
        }
        return new CachedBodyHttpServletRequest(request);
    }

    private String resolvePrincipalScope(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof User user) {
            return "user:" + user.getId();
        }
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return "user:" + auth.getName();
        }
        return "ip:" + IpUtils.getClientIp(request);
    }

    private String computeRequestHash(CachedBodyHttpServletRequest request) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String method = request.getMethod() != null ? request.getMethod().toUpperCase() : "";
            String uri = request.getRequestURI() != null ? request.getRequestURI() : "";
            String query = request.getQueryString() != null ? request.getQueryString() : "";
            String meta = method + "|" + uri + "|" + query + "|";
            digest.update(meta.getBytes(StandardCharsets.UTF_8));
            digest.update(request.getCachedBody());
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private void writeErrorResponse(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        String traceId = MDC.get(MDC_TRACE_ID_KEY);
        ApiResponse<Object> apiResponse = new ApiResponse<>(message, null, status.value(), traceId);
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }

    private long toMillis(long elapsedNs) {
        return elapsedNs / 1_000_000L;
    }
}

