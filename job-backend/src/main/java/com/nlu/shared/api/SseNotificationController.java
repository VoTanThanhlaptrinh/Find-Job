package com.nlu.shared.api;

import com.nlu.identity.domain.model.CurrentUser;
import com.nlu.identity.domain.model.User;
import com.nlu.shared.application.SseEmitterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
public class SseNotificationController {
    private final SseEmitterService sseEmitterService;

    @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> connect(@CurrentUser User currentUser) {
        var emitter = sseEmitterService.createEmitter(currentUser);
        return ResponseEntity.ok()
                .header("X-Accel-Buffering", "no")
                .header("Cache-Control", "no-cache, no-transform")
                .header("Connection", "keep-alive")
                .body(emitter);
    }
}
