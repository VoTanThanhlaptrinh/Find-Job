package com.job_web.service.notification;

import com.job_web.dto.common.ApiResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.security.Principal;

public interface SseNotificationService {
    
    /**
     * Đăng ký client SSE để nhận thông báo cho resume cụ thể
     * Kiểm tra quyền sở hữu resume trước khi cho phép subscribe
     */
    ApiResponse<SseEmitter> subscribe(Long resumeId, Principal principal);
    
    /**
     * Gửi thông báo đến client đang subscribe resumeId
     */
    void sendNotification(Long resumeId, String message);
    
    /**
     * Xóa emitter khi client ngắt kết nối
     */
    void removeEmitter(Long resumeId);
    
    /**
     * Kiểm tra resume có thuộc về user không
     */
    boolean isResumeOwnedByUser(Long resumeId, String userEmail);
}
