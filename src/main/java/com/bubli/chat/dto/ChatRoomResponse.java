package com.bubli.chat.dto;

import com.bubli.chat.type.ChatRoomStatus;
import com.bubli.chat.type.ChatType;

import java.time.Instant;
import java.util.UUID;

public record ChatRoomResponse(
		UUID id,
		UUID roomId,
		ChatType chatType,
		String name,
		ChatRoomStatus status,
		Instant createdAt,
		Instant updatedAt
) {
	public static ChatRoomResponse from(ChatRoomResult result) {
		return new ChatRoomResponse(
				result.id(),
				result.roomId(),
				result.chatType(),
				result.name(),
				result.status(),
				result.createdAt(),
				result.updatedAt()
		);
	}
}
