package com.bubli.user.dto;

import java.time.Instant;
import java.util.UUID;

public record FriendResponse(
		UUID userId,
		String bubliId,
		String name,
		String avatarUrl,
		Instant acceptedAt
) {
}
