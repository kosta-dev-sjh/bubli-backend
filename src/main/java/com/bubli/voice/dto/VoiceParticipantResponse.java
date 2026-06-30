package com.bubli.voice.dto;

import java.time.Instant;
import java.util.UUID;

public record VoiceParticipantResponse(
        UUID id,
        UUID userId,
        String userName,
        String status,
        Instant joinedAt,
        Instant leftAt
) {}
