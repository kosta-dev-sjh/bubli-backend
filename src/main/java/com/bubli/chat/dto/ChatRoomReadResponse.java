package com.bubli.chat.dto;

import java.time.Instant;
import java.util.UUID;

public record ChatRoomReadResponse(
		UUID chatRoomId,
		Long lastReadSequence,
		Instant lastReadAt
) {
}
