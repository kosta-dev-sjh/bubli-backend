package com.bubli.voice.service;

import com.bubli.voice.config.LiveKitProperties;
import com.bubli.voice.dto.LiveKitWebhookEvent;
import com.bubli.voice.entity.VoiceParticipant;
import com.bubli.voice.repository.VoiceParticipantRepository;
import com.bubli.voice.repository.VoiceRoomRepository;
import com.bubli.voice.type.VoiceParticipantStatus;
import com.bubli.voice.type.VoiceRoomStatus;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LiveKitWebhookService {

    private final VoiceRoomRepository voiceRoomRepository;
    private final VoiceParticipantRepository voiceParticipantRepository;
    private final LiveKitProperties liveKitProperties;

    public boolean verifySignature(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return false;
        }
        try {
            String token = authHeader.substring(7);
            SecretKey key = Keys.hmacShaKeyFor(liveKitProperties.apiSecret().getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            return liveKitProperties.apiKey().equals(claims.getIssuer());
        } catch (Exception e) {
            log.warn("LiveKit webhook signature verification failed: {}", e.getMessage());
            return false;
        }
    }

    @Transactional
    public void handleEvent(LiveKitWebhookEvent event) {
        if (event.event() == null || event.room() == null) return;

        switch (event.event()) {
            case "participant_left" -> handleParticipantLeft(event);
            case "room_finished" -> handleRoomFinished(event);
            default -> log.debug("Unhandled LiveKit event: {}", event.event());
        }
    }

    private void handleParticipantLeft(LiveKitWebhookEvent event) {
        if (event.participant() == null) return;

        String roomName = event.room().name();
        String identity = event.participant().identity();

        try {
            UUID userId = UUID.fromString(identity);
            voiceRoomRepository.findByLivekitRoomName(roomName).ifPresent(room -> {
                voiceParticipantRepository.findByVoiceRoomIdAndUserId(room.getId(), userId)
                        .ifPresent(VoiceParticipant::leave);
            });
        } catch (IllegalArgumentException e) {
            log.warn("Invalid participant identity (not UUID): {}", identity);
        }
    }

    private void handleRoomFinished(LiveKitWebhookEvent event) {
        String roomName = event.room().name();

        voiceRoomRepository.findByLivekitRoomName(roomName).ifPresent(room -> {
            if (room.getStatus() == VoiceRoomStatus.ENDED) return;

            voiceParticipantRepository.findByVoiceRoomIdAndStatus(room.getId(), VoiceParticipantStatus.JOINED)
                    .forEach(VoiceParticipant::leave);
            room.end();
        });
    }
}
