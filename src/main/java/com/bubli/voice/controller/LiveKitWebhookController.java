package com.bubli.voice.controller;

import com.bubli.voice.dto.LiveKitWebhookEvent;
import com.bubli.voice.service.LiveKitWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/voice/webhook")
@RequiredArgsConstructor
public class LiveKitWebhookController {

    private final LiveKitWebhookService liveKitWebhookService;

    @PostMapping("/livekit")
    public ResponseEntity<Void> handleWebhook(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody LiveKitWebhookEvent event
    ) {
        if (!liveKitWebhookService.verifySignature(authHeader)) {
            log.warn("LiveKit webhook signature verification failed");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        liveKitWebhookService.handleEvent(event);
        return ResponseEntity.ok().build();
    }
}
