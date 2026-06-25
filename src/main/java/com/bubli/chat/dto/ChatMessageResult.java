package com.bubli.chat.dto;

import com.bubli.chat.entity.ChatMessage;
import com.bubli.chat.type.MessageType;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

public record ChatMessageResult(
		UUID id,
		UUID chatRoomId,
		String senderType,
		UUID senderId,
		String senderName,
		String clientMessageId,
		Long roomSequence,
		MessageType messageType,
		JsonNode body,
		UUID resourceId,
		Instant createdAt
) {
}
