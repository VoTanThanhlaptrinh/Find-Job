package com.nlu.shared.application.impl;

import com.nlu.applicationProcess.domain.repository.ResumeRepository;
import com.nlu.shared.domain.exception.ForbiddenException;
import com.nlu.shared.domain.exception.UnauthorizedException;
import com.nlu.applicationProcess.domain.model.Resume;
import com.nlu.identity.domain.model.User;
import com.nlu.shared.application.SseNotificationService;
import com.nlu.shared.domain.model.SseMessagePayload;
import com.nlu.shared.utils.MessageUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class SseNotificationServiceImpl implements SseNotificationService {

    private static final long SSE_TIMEOUT = 30 * 60 * 1000L; // Timeout 30 phút
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    private static final String MDC_USER_ID = "userId";

    @Override
    public SseEmitter subscribe(User user, String eventName) {
        if (user == null) {
            throw new UnauthorizedException(MessageUtils.getMessage("message.unauthorized"));
        }

        try {
            MDC.put(MDC_USER_ID, String.valueOf(user.getId()));

            String routingKey = buildRoutingKey(user.getId(), eventName);

            SseEmitter existingEmitter = emitters.get(routingKey);
            if (existingEmitter != null) {
                existingEmitter.complete();
                emitters.remove(routingKey);
                log.info("Closed existing SSE connection for routing key: {}", routingKey);
            }

            SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
            emitters.put(routingKey, emitter);

            // Các callback này tự động kích hoạt khi Frontend gọi AbortController.abort()
            emitter.onCompletion(() -> {
                log.info("SSE connection completed (Client disconnected)");
                emitters.remove(routingKey);
            });

            emitter.onTimeout(() -> {
                log.info("SSE connection timed out");
                emitter.complete();
                emitters.remove(routingKey);
            });

            emitter.onError(ex -> {
                log.warn("SSE connection error");
                emitters.remove(routingKey);
            });

            // 1. Gửi tín hiệu kết nối đầu tiên
            try {
                emitter.send(SseEmitter.event()
                        .name("connect")
                        .data(SseMessagePayload.builder()
                                .status("CONNECTED")
                                .message("Connected to SSE for userId: ")
                                .build(), MediaType.APPLICATION_JSON));
            } catch (IOException e) {
                log.warn("Failed to send initial SSE event");
                emitters.remove(routingKey);
                throw new RuntimeException(MessageUtils.getMessage("notification.sse.failed"));
            }
            return emitter;
        } finally {
            MDC.remove(MDC_USER_ID);
        }
    }

    @Override
    public void sendNotification(long userId, String eventName, SseMessagePayload<?> payload) {
        String routingKey = buildRoutingKey(userId, eventName);
        SseEmitter emitter = emitters.get(routingKey);
        if (emitter == null) {
            log.info("No active SSE connection for routing key: {} — stored as pending", routingKey);
            return;
        }

        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(payload, MediaType.APPLICATION_JSON));
            log.info("SSE notification ({}) sent for routing key: {}", eventName, routingKey);
        } catch (IOException e) {
            log.warn("Failed to send SSE notification for routing key: {}", routingKey);
            emitters.remove(routingKey);
            emitter.completeWithError(e);
        }
    }

    @Override
    public void removeEmitter(long userId, String eventName) {
        String routingKey = buildRoutingKey(userId, eventName);
        SseEmitter emitter = emitters.remove(routingKey);
        if (emitter != null) {
            emitter.complete();
            log.info("SSE emitter removed manually for routing key: {}", routingKey);
        }
    }

    /* HELPER METHOD */
    private String buildRoutingKey(long userId, String eventName) {
        return userId + "-" + eventName;
    }
}