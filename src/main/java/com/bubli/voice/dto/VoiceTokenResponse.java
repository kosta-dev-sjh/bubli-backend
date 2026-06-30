package com.bubli.voice.dto;

import java.time.Instant;
import java.util.UUID;

public record VoiceTokenResponse(
        String serverUrl,
        String token,
        UUID voiceRoomId,
        UUID participantId,
        Instant expiresAt
) {}
