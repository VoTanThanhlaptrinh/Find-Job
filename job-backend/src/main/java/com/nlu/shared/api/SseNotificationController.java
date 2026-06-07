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

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> subscribe(@RequestParam("event") String event, @CurrentUser User currentUser) {
        var res = sseNotificationService.subscribe(currentUser, event);
        return ResponseEntity.ok()
                .header("X-Accel-Buffering", "no")
                .header("Cache-Control", "no-cache, no-transform")
                .header("Connection", "keep-alive")
                .body(res);
    }
}
