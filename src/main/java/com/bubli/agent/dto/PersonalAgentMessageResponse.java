package com.bubli.agent.dto;

import com.bubli.chat.type.MessageType;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

public record PersonalAgentMessageResponse(
		String senderType,
		MessageType messageType,
		JsonNode body,
		Instant createdAt
) {
}
