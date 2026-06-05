package com.nlu.shared.api;

import com.nlu.shared.domain.model.ApiResponse;
import com.nlu.identity.domain.model.CurrentUser;
import com.nlu.identity.domain.model.User;
import com.nlu.shared.application.SseNotificationService;
import com.nlu.shared.utils.MessageUtils;
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
        return ResponseEntity.ok(res);
    }

    @PostMapping("/{resumeId}/send")
    public ResponseEntity<ApiResponse<String>> sendNotification(
            @PathVariable Long resumeId,
            @RequestBody String message) {
        sseNotificationService.sendNotification(resumeId, message);
        return ResponseEntity.ok(new ApiResponse<>(MessageUtils.getMessage("notification.sent"), null, HttpStatus.OK.value()));
    }
}
