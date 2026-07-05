package com.bubli.chat.dto;

import java.util.UUID;

public record ChatTypingEvent(
		UUID chatRoomId,
		boolean typing,
		UUID userId,
		String userName
) {
}
