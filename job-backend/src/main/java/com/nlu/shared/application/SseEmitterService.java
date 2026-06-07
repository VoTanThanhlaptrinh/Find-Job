package com.nlu.shared.application;

import com.nlu.identity.domain.model.User;
import com.nlu.shared.domain.model.SseMessagePayload;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Dịch vụ quản lý kết nối SSE tổng quát.
 * Mỗi user chỉ giữ 1 kết nối SSE duy nhất;
 * server push nhiều event types khác nhau trên kết nối đó.
 */
public interface SseEmitterService {

    /**
     * Tạo SseEmitter mới cho user. Nếu user đã có emitter cũ, emitter cũ sẽ bị đóng.
     */
    SseEmitter createEmitter(User user);

    /**
     * Gửi event đến user đang có kết nối SSE.
     * Bất kỳ service nào cũng có thể gọi method này.
     *
     * @param userId    ID của user nhận event
     * @param eventName tên event (ví dụ: "resume-process", "notification", "job-match")
     * @param payload   dữ liệu gửi kèm
     */
    <T> void sendEvent(long userId, String eventName, SseMessagePayload<T> payload);

    /**
     * Xóa emitter của user (khi logout hoặc cần cleanup thủ công).
     */
    void removeEmitter(long userId);
}
