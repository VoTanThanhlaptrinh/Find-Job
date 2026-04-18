package com.job_web.service.notification.impl;

import com.job_web.data.ResumeRepository;
import com.job_web.dto.common.ApiResponse;
import com.job_web.models.Resume;
import com.job_web.models.User;
import com.job_web.service.notification.SseNotificationService;
import com.job_web.utills.MessageUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class SseNotificationServiceImpl implements SseNotificationService {
    
    private static final Long SSE_TIMEOUT = 30 * 60 * 1000L;
    
    private final ResumeRepository resumeRepository;
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    private static final String MDC_USER_ID = "userId";
    private static final String MDC_CV_ID = "cvId";

    @Override
    public ApiResponse<SseEmitter> subscribe(Long resumeId, User user) {
        if (user == null) {
            return new ApiResponse<>(MessageUtils.getMessage("message.unauthorized"), null, HttpStatus.UNAUTHORIZED.value());
        }

        try {
            MDC.put(MDC_USER_ID, String.valueOf(user.getId()));
            MDC.put(MDC_CV_ID, String.valueOf(resumeId));

            if (!isResumeOwnedByUser(resumeId, user.getEmail())) {
                log.warn("SSE subscribe forbidden — user: {} does not own CV: {}", user.getId(), resumeId);
                return new ApiResponse<>(MessageUtils.getMessage("resume.access.forbidden"), null, HttpStatus.FORBIDDEN.value());
            }

            // Close existing emitter if present
            SseEmitter existingEmitter = emitters.get(resumeId);
            if (existingEmitter != null) {
                existingEmitter.complete();
                emitters.remove(resumeId);
                log.info("Closed existing SSE connection for CV: {}", resumeId);
            }

            SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
            emitters.put(resumeId, emitter);

            emitter.onCompletion(() -> {
                log.info("SSE connection completed for CV: {}", resumeId);
                emitters.remove(resumeId);
            });

            emitter.onTimeout(() -> {
                log.info("SSE connection timed out for CV: {}", resumeId);
                emitter.complete();
                emitters.remove(resumeId);
            });

            emitter.onError(ex -> {
                log.warn("SSE connection error for CV: {}", resumeId);
                emitters.remove(resumeId);
            });

            try {
                emitter.send(SseEmitter.event()
                        .name("connect")
                        .data("Connected to SSE for resumeId: " + resumeId));
            } catch (IOException e) {
                log.warn("Failed to send initial SSE event for CV: {}", resumeId);
                emitters.remove(resumeId);
                return new ApiResponse<>(MessageUtils.getMessage("notification.sse.failed"), null, HttpStatus.INTERNAL_SERVER_ERROR.value());
            }

            log.info("SSE client subscribed for CV: {} by user: {}", resumeId, user.getId());
            return new ApiResponse<>(MessageUtils.getMessage("message.success"), emitter, HttpStatus.OK.value());
        } finally {
            MDC.remove(MDC_USER_ID);
            MDC.remove(MDC_CV_ID);
        }
    }

    @Override
    public void sendNotification(Long resumeId, String message) {
        SseEmitter emitter = emitters.get(resumeId);
        if (emitter == null) {
            log.warn("No active SSE connection for CV: {}", resumeId);
            return;
        }
        
        try {
            emitter.send(SseEmitter.event()
                    .name("notification")
                    .data(message));
            log.info("SSE notification sent for CV: {}", resumeId);
        } catch (IOException e) {
            log.warn("Failed to send SSE notification for CV: {} — removing emitter", resumeId);
            emitters.remove(resumeId);
            emitter.completeWithError(e);
        }
    }

    @Override
    public void removeEmitter(Long resumeId) {
        SseEmitter emitter = emitters.remove(resumeId);
        if (emitter != null) {
            emitter.complete();
            log.info("SSE emitter removed for CV: {}", resumeId);
        }
    }

    @Override
    public boolean isResumeOwnedByUser(Long resumeId, String userEmail) {
        // Read-only ownership check — no logging needed.
        Optional<Resume> resumeOpt = resumeRepository.findById(resumeId);
        if (resumeOpt.isEmpty()) {
            return false;
        }
        Resume resume = resumeOpt.get();
        return resume.getUser() != null && userEmail.equals(resume.getUser().getEmail());
    }
}
