package com.bubli.chat.dto;

import com.bubli.chat.entity.ChatRoom;
import com.bubli.chat.type.ChatRoomStatus;
import com.bubli.chat.type.ChatType;

import java.time.Instant;
import java.util.UUID;

public record ChatRoomResult(
		UUID id,
		UUID roomId,
		ChatType chatType,
		String name,
		ChatRoomStatus status,
		Instant createdAt,
		Instant updatedAt
) {
	public static ChatRoomResult from(ChatRoom chatRoom) {
		return new ChatRoomResult(
				chatRoom.getId(),
				chatRoom.getRoomId(),
				chatRoom.getChatType(),
				chatRoom.getName(),
				chatRoom.getStatus(),
				chatRoom.getCreatedAt(),
				chatRoom.getUpdatedAt()
		);
	}
}
