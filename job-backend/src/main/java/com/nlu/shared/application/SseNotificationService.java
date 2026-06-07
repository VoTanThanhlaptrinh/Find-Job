package com.nlu.shared.application;

import com.nlu.identity.domain.model.User;
import com.nlu.shared.domain.model.SseMessagePayload;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface SseNotificationService {
    
    /**
     * Đăng ký client SSE để nhận thông báo cho resume cụ thể
     * Kiểm tra quyền sở hữu resume trước khi cho phép subscribe
     */
    SseEmitter subscribe(User user, String eventName);
    
    /**
     * Gửi thông báo đến client đang subscribe resumeId
     */
    void sendNotification(long userId, String eventName, SseMessagePayload<?> payload);
    
    /**
     * Xóa emitter khi client ngắt kết nối
     */
    void removeEmitter(long userId, String eventName);

}
