package com.bubli.project.dto;

import java.time.Instant;
import java.util.UUID;

public record InviteLinkResponse(
		String token,
		UUID roomId,
		String roomName,
		String inviterName,
		Instant expiresAt,
		boolean expired
) {
}
