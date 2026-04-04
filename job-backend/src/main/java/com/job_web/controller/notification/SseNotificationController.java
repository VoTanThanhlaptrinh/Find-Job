package com.job_web.controller.notification;

import com.job_web.dto.common.ApiResponse;
import com.job_web.models.CurrentUser;
import com.job_web.models.User;
import com.job_web.service.notification.SseNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class SseNotificationController {
    private final SseNotificationService sseNotificationService;

    @GetMapping(value = "/{resumeId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> subscribe(@PathVariable Long resumeId, @CurrentUser User currentUser) {
        var res = sseNotificationService.subscribe(resumeId, currentUser);
        return ResponseEntity.status(res.getStatus()).body(res.data());
    }

    @PostMapping("/{resumeId}/send")
    public ResponseEntity<ApiResponse<String>> sendNotification(
            @PathVariable Long resumeId,
            @RequestBody String message) {
        sseNotificationService.sendNotification(resumeId, message);
        return ResponseEntity.ok(new ApiResponse<>("Notification sent", null, HttpStatus.OK.value()));
    }
}
