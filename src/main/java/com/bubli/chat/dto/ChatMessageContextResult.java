package com.bubli.chat.dto;

import com.bubli.chat.entity.ChatMessage;
import com.bubli.chat.type.MessageType;

import java.time.Instant;
import java.util.UUID;

public record ChatMessageContextResult(
		UUID id,
		Long roomSequence,
		MessageType messageType,
		String body,
		UUID resourceId,
		Instant createdAt
) {

	public static ChatMessageContextResult from(ChatMessage message) {
		return new ChatMessageContextResult(
				message.getId(),
				message.getRoomSequence(),
				message.getMessageType(),
				message.getBody(),
				message.getResourceId(),
				message.getCreatedAt()
		);
	}
}
