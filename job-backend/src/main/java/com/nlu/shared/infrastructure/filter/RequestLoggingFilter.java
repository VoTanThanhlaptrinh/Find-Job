package com.nlu.shared.infrastructure.filter;

import com.nlu.shared.utils.PayloadMaskingUtil;
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

        // [FIX 1]: Nhận diện SSE linh hoạt và an toàn hơn
        String acceptHeader = request.getHeader("Accept");
        boolean isSseRequest = request.getRequestURI().contains("/sse/") ||
                (acceptHeader != null && acceptHeader.toLowerCase().contains("text/event-stream"));

        ContentCachingResponseWrapper wrappedResponse =
                (!isSseRequest && !(response instanceof ContentCachingResponseWrapper))
                        ? new ContentCachingResponseWrapper(response)
                        : (response instanceof ContentCachingResponseWrapper)
                        ? (ContentCachingResponseWrapper) response
                        : null;

        long startTime = System.currentTimeMillis();

        HttpServletResponse effectiveResponse = (wrappedResponse != null) ? wrappedResponse : response;

        try {
            // ── 3. Put trace ID into MDC ────────────────────────────────────
            org.slf4j.MDC.put(MDC_TRACE_ID_KEY, traceId);

            // ── 4. Echo the trace ID back in the response header ────────────
            effectiveResponse.setHeader(CORRELATION_HEADER, traceId);

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
            filterChain.doFilter(wrappedRequest, effectiveResponse);

        } finally {
            // ── 8. ALWAYS clear MDC (Làm ĐẦU TIÊN để chống thread pool leak) ──
            org.slf4j.MDC.clear();

            // [FIX 2]: KIỂM TRA TRẠNG THÁI ASYNC
            // Nếu Request đang chạy bất đồng bộ (SseEmitter), luồng chính đã xong việc,
            // tuyệt đối KHÔNG thực hiện các lệnh đóng/flush response ở bên dưới.
            if (request.isAsyncStarted()) {
                return;
            }

            // ── 9. Log request body (chỉ chạy với API đồng bộ) ───────────────
            logRequestBody(wrappedRequest);

            // ── 10. Log response summary ─────────────────────────────────────
            long duration = System.currentTimeMillis() - startTime;
            int status = effectiveResponse.getStatus();

            if (status >= 500) {
                log.error("◄ [{} {}] completed in {}ms with status {}",
                        wrappedRequest.getMethod(), wrappedRequest.getRequestURI(), duration, status);
            } else if (status >= 400) {
                log.warn("◄ [{} {}] completed in {}ms with status {}",
                        wrappedRequest.getMethod(), wrappedRequest.getRequestURI(), duration, status);
            } else {
                log.info("◄ [{} {}] completed in {}ms with status {}",
                        wrappedRequest.getMethod(), wrappedRequest.getRequestURI(), duration, status);
            }

            // ── 11. Copy cached body back to the actual response ────────────
            if (wrappedResponse != null) {
                wrappedResponse.copyBodyToResponse();
            }
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
