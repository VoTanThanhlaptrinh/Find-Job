package com.job_web.service.notification.impl;

import com.job_web.data.ResumeRepository;
import com.job_web.dto.common.ApiResponse;
import com.job_web.models.Resume;
import com.job_web.models.User;
import com.job_web.service.notification.SseNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Override
    public ApiResponse<SseEmitter> subscribe(Long resumeId, User user) {
        // Kiểm tra đăng nhập
        if (user == null) {
            return new ApiResponse<>("You are not logged in.", null, HttpStatus.UNAUTHORIZED.value());
        }
        
        // Kiểm tra quyền sở hữu resume
        if (!isResumeOwnedByUser(resumeId, user.getEmail())) {
            return new ApiResponse<>("You do not have permission to access this resume.", null, HttpStatus.FORBIDDEN.value());
        }
        
        // Đóng emitter cũ nếu có
        SseEmitter existingEmitter = emitters.get(resumeId);
        if (existingEmitter != null) {
            existingEmitter.complete();
            emitters.remove(resumeId);
        }
        
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        emitters.put(resumeId, emitter);
        
        emitter.onCompletion(() -> {
            log.info("SSE connection completed for resumeId: {}", resumeId);
            emitters.remove(resumeId);
        });
        
        emitter.onTimeout(() -> {
            log.info("SSE connection timeout for resumeId: {}", resumeId);
            emitter.complete();
            emitters.remove(resumeId);
        });
        
        emitter.onError(ex -> {
            log.error("SSE connection error for resumeId: {}", resumeId, ex);
            emitters.remove(resumeId);
        });
        
        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("Connected to SSE for resumeId: " + resumeId));
        } catch (IOException e) {
            log.error("Error sending initial SSE event for resumeId: {}", resumeId, e);
            emitters.remove(resumeId);
            return new ApiResponse<>("Failed to establish SSE connection.", null, HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        log.info("SSE client subscribed for resumeId: {} by user: {}", resumeId, user.getEmail());
        return new ApiResponse<>("success", emitter, HttpStatus.OK.value());
    }

    @Override
    public void sendNotification(Long resumeId, String message) {
        SseEmitter emitter = emitters.get(resumeId);
        if (emitter == null) {
            log.warn("No active SSE connection for resumeId: {}", resumeId);
            return;
        }
        
        try {
            emitter.send(SseEmitter.event()
                    .name("notification")
                    .data(message));
            log.info("Sent SSE notification for resumeId: {}, message: {}", resumeId, message);
        } catch (IOException e) {
            log.error("Error sending SSE notification for resumeId: {}", resumeId, e);
            emitters.remove(resumeId);
            emitter.completeWithError(e);
        }
    }

    @Override
    public void removeEmitter(Long resumeId) {
        SseEmitter emitter = emitters.remove(resumeId);
        if (emitter != null) {
            emitter.complete();
            log.info("Removed SSE emitter for resumeId: {}", resumeId);
        }
    }

    @Override
    public boolean isResumeOwnedByUser(Long resumeId, String userEmail) {
        Optional<Resume> resumeOpt = resumeRepository.findById(resumeId);
        if (resumeOpt.isEmpty()) {
            return false;
        }
        Resume resume = resumeOpt.get();
        return resume.getUser() != null && userEmail.equals(resume.getUser().getEmail());
    }
}
