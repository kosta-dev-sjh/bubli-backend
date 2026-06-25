package com.bubli.chat.dto;

import com.bubli.chat.type.MessageType;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

public record ChatMessageResponse(
		UUID id,
		UUID chatRoomId,
		ChatSenderResponse sender,
		String clientMessageId,
		Long roomSequence,
		MessageType messageType,
		JsonNode body,
		UUID resourceId,
		Instant createdAt
) {
	public static ChatMessageResponse from(ChatMessageResult result) {
		return new ChatMessageResponse(
				result.id(),
				result.chatRoomId(),
				new ChatSenderResponse(result.senderType(), result.senderId(), result.senderName()),
				result.clientMessageId(),
				result.roomSequence(),
				result.messageType(),
				result.body(),
				result.resourceId(),
				result.createdAt()
		);
	}

	public record ChatSenderResponse(
			String type,
			UUID id,
			String name
	) {
	}
}
