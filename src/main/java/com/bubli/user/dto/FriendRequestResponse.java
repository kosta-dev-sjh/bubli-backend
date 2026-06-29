package com.bubli.user.dto;

import java.time.Instant;
import java.util.UUID;

public record FriendRequestResponse(
		UUID id,
		UUID requesterId,
		String requesterName,
		String requesterBubliId,
		UUID receiverId,
		String receiverName,
		String receiverBubliId,
		String status,
		Instant createdAt
) {
}
