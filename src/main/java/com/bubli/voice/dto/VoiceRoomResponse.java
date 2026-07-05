package com.bubli.voice.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record VoiceRoomResponse(
        UUID id,
        UUID roomId,
        UUID chatRoomId,
        String livekitRoomName,
        String status,
        List<VoiceParticipantResponse> participants,
        Instant createdAt,
        UUID createdByUserId
) {}
