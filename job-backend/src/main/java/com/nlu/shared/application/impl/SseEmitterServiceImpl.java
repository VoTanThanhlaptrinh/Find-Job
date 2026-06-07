package com.nlu.shared.application.impl;

import com.nlu.identity.domain.model.User;
import com.nlu.shared.application.SseEmitterService;
import com.nlu.shared.domain.exception.UnauthorizedException;
import com.nlu.shared.domain.model.SseMessagePayload;
import com.nlu.shared.utils.MessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SseEmitterServiceImpl implements SseEmitterService {

    private static final long SSE_TIMEOUT = 30 * 60 * 1000L; // 30 phút

    /**
     * Mỗi user chỉ có tối đa 1 emitter. Key = userId.
     */
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    @Override
    public SseEmitter createEmitter(User user) {
        if (user == null) {
            throw new UnauthorizedException(MessageUtils.getMessage("message.unauthorized"));
        }

        long userId = user.getId();

        // Đóng emitter cũ nếu có (ví dụ user mở tab mới)
        SseEmitter existing = emitters.remove(userId);
        if (existing != null) {
            existing.complete();
            log.info("Closed existing SSE emitter for userId={}", userId);
        }

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        emitters.put(userId, emitter);

        // Callback cleanup — tự động xóa khỏi map khi kết nối kết thúc
        emitter.onCompletion(() -> {
            emitters.remove(userId, emitter); // chỉ xóa nếu vẫn là emitter này
            log.debug("SSE completed for userId={}", userId);
        });
        emitter.onTimeout(() -> {
            emitters.remove(userId, emitter);
            log.info("SSE timed out for userId={}", userId);
            emitter.complete();
        });
        emitter.onError(ex -> {
            emitters.remove(userId, emitter);
            log.warn("SSE error for userId={}: {}", userId, ex.getMessage());
        });

        // Gửi event kết nối thành công ngay lập tức
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(SseMessagePayload.builder()
                            .status("CONNECTED")
                            .message("SSE connection established")
                            .build(), MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            emitters.remove(userId, emitter);
            log.warn("Failed to send initial SSE event for userId={}", userId);
            throw new RuntimeException(MessageUtils.getMessage("notification.sse.failed"));
        }

        log.info("SSE emitter created for userId={}", userId);
        return emitter;
    }

    /* ================================================================
     *  GỬI EVENT
     * ================================================================ */

    @Override
    public <T> void sendEvent(long userId, String eventName, SseMessagePayload<T> payload) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) {
            log.debug("No active SSE connection for userId={}, event '{}' dropped", userId, eventName);
            return;
        }

        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(payload, MediaType.APPLICATION_JSON));
            log.debug("SSE event '{}' sent to userId={}", eventName, userId);
        } catch (IOException e) {
            log.warn("Failed to send SSE event '{}' to userId={}", eventName, userId);
            emitters.remove(userId, emitter);
            emitter.completeWithError(e);
        }
    }

    /* ================================================================
     *  XÓA EMITTER
     * ================================================================ */

    @Override
    public void removeEmitter(long userId) {
        SseEmitter emitter = emitters.remove(userId);
        if (emitter != null) {
            emitter.complete();
            log.info("SSE emitter removed for userId={}", userId);
        }
    }

    /* ================================================================
     *  HEARTBEAT — Giữ kết nối sống, phát hiện dead connections
     * ================================================================ */

    @Scheduled(fixedRate = 30_000) // Mỗi 30 giây
    public void heartbeat() {
        if (emitters.isEmpty()) {
            return;
        }

        List<Long> deadEmitters = new ArrayList<>();

        emitters.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event().comment("ping"));
            } catch (IOException e) {
                log.debug("Heartbeat failed for userId={}, marking as dead", userId);
                deadEmitters.add(userId);
            }
        });

        // Cleanup dead emitters
        for (Long userId : deadEmitters) {
            SseEmitter emitter = emitters.remove(userId);
            if (emitter != null) {
                emitter.completeWithError(new IOException("Heartbeat failed"));
            }
        }

        if (!deadEmitters.isEmpty()) {
            log.info("Heartbeat cleanup: removed {} dead connections, {} active", deadEmitters.size(), emitters.size());
        }
    }
}
