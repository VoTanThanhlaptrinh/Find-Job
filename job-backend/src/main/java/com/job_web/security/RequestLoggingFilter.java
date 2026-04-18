package com.job_web.security;

import com.job_web.utills.PayloadMaskingUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

/**
 * Centralized HTTP request/response logging filter for the Controller layer.
 * <p>
 * This filter is positioned at the very top of the filter chain
 * ({@link Ordered#HIGHEST_PRECEDENCE}) so that <strong>every</strong> request
 * flowing through the application is instrumented with:
 * <ul>
 *   <li>A <strong>Trace ID</strong> (propagated from {@code X-Correlation-ID}
 *       header, or newly generated UUID) injected into SLF4J MDC.</li>
 *   <li><strong>Request metadata</strong> — HTTP method, URI, client IP,
 *       User-Agent.</li>
 *   <li><strong>Response summary</strong> — HTTP status code and total
 *       execution time in milliseconds.</li>
 *   <li><strong>Sanitized request body</strong> — logged at DEBUG level with
 *       all PII/security values masked via {@link PayloadMaskingUtil}.</li>
 * </ul>
 * <p>
 * The filter wraps the original request and response in
 * {@link ContentCachingRequestWrapper} / {@link ContentCachingResponseWrapper}
 * to allow reading the body without consuming the stream. The response body
 * itself is <strong>never</strong> logged to avoid dumping large payloads
 * (CV files, paginated lists).
 *
 * <h3>Thread Safety</h3>
 * MDC context is <strong>always</strong> cleared in a {@code finally} block to
 * prevent leaking state across pooled threads (e.g. Tomcat NIO).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final String CORRELATION_HEADER = "X-Correlation-ID";
    private static final String MDC_TRACE_ID_KEY = "traceId";

    /**
     * Content types that are eligible for body logging.
     */
    private static final Set<String> LOGGABLE_CONTENT_TYPES = Set.of(
            "application/json",
            "application/x-www-form-urlencoded",
            "text/plain"
    );

    /**
     * Maximum body size (in bytes) that will be logged. Payloads larger than
     * this threshold are skipped to protect log storage.
     */
    private static final int MAX_BODY_LOG_SIZE = 10_240; // 10 KB

    // ──────────────────────────────────────────────────────────────────────────

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // ── 1. Trace ID: propagate or generate ──────────────────────────────
        String traceId = request.getHeader(CORRELATION_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }

        // ── 2. Wrap request & response for caching ─────────────────────────
        ContentCachingRequestWrapper wrappedRequest =
                (request instanceof ContentCachingRequestWrapper)
                        ? (ContentCachingRequestWrapper) request
                        : new ContentCachingRequestWrapper(request, MAX_BODY_LOG_SIZE);

        ContentCachingResponseWrapper wrappedResponse =
                (response instanceof ContentCachingResponseWrapper)
                        ? (ContentCachingResponseWrapper) response
                        : new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();

        try {
            // ── 3. Put trace ID into MDC ────────────────────────────────────
            org.slf4j.MDC.put(MDC_TRACE_ID_KEY, traceId);

            // ── 4. Echo the trace ID back in the response header ────────────
            wrappedResponse.setHeader(CORRELATION_HEADER, traceId);

            // ── 5. Log incoming request summary ─────────────────────────────
            String clientIp = resolveClientIp(wrappedRequest);
            String userAgent = wrappedRequest.getHeader("User-Agent");

            log.info("► [{}  {}] from {} | User-Agent: {}",
                    wrappedRequest.getMethod(),
                    wrappedRequest.getRequestURI(),
                    clientIp,
                    userAgent != null ? userAgent : "unknown");

            // ── 6. Log sanitized query parameters at DEBUG level ────────────
            if (log.isDebugEnabled() && wrappedRequest.getQueryString() != null) {
                log.debug("  Query: {}", PayloadMaskingUtil.maskSensitiveData(
                        wrappedRequest.getQueryString()));
            }

            // ── 7. Continue the filter chain ────────────────────────────────
            filterChain.doFilter(wrappedRequest, wrappedResponse);

        } finally {
            // ── 8. Log request body (available after chain execution) ───────
            logRequestBody(wrappedRequest);

            // ── 9. Log response summary ─────────────────────────────────────
            long duration = System.currentTimeMillis() - startTime;
            int status = wrappedResponse.getStatus();

            if (status >= 500) {
                log.error("◄ [{} {}] completed in {}ms with status {}",
                        wrappedRequest.getMethod(),
                        wrappedRequest.getRequestURI(),
                        duration,
                        status);
            } else if (status >= 400) {
                log.warn("◄ [{} {}] completed in {}ms with status {}",
                        wrappedRequest.getMethod(),
                        wrappedRequest.getRequestURI(),
                        duration,
                        status);
            } else {
                log.info("◄ [{} {}] completed in {}ms with status {}",
                        wrappedRequest.getMethod(),
                        wrappedRequest.getRequestURI(),
                        duration,
                        status);
            }

            // ── 10. Copy cached body back to the actual response ────────────
            wrappedResponse.copyBodyToResponse();

            // ── 11. ALWAYS clear MDC (prevents thread-pool leaks) ───────────
            org.slf4j.MDC.clear();
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ──────────────────────────────────────────────────────────────────────────

    private void logRequestBody(ContentCachingRequestWrapper request) {
        if (!log.isDebugEnabled()) {
            return;
        }

        String contentType = request.getContentType();
        if (contentType == null || !isLoggableContentType(contentType)) {
            return;
        }

        byte[] bodyBytes = request.getContentAsByteArray();
        if (bodyBytes.length == 0) {
            return;
        }

        if (bodyBytes.length > MAX_BODY_LOG_SIZE) {
            log.debug("  Body: [truncated — {} bytes]", bodyBytes.length);
            return;
        }

        String body = new String(bodyBytes, StandardCharsets.UTF_8);
        log.debug("  Body: {}", PayloadMaskingUtil.maskSensitiveData(body));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isBlank()) {
            return ip.trim();
        }
        return request.getRemoteAddr();
    }

    private boolean isLoggableContentType(String contentType) {
        String lower = contentType.toLowerCase();
        return LOGGABLE_CONTENT_TYPES.stream().anyMatch(lower::contains);
    }
}
