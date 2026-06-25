package com.bubli.chat.dto;

import com.bubli.chat.type.MessageType;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.UUID;

public record SendChatMessageCommand(
		String clientMessageId,
		MessageType messageType,
		JsonNode body,
		UUID resourceId
) {
}
