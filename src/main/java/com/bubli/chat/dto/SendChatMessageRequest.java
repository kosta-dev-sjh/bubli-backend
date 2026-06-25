package com.bubli.chat.dto;

import com.bubli.chat.type.MessageType;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record SendChatMessageRequest(
		@NotBlank(message = "클라이언트 메시지 ID는 필수입니다.")
		@Size(max = 120, message = "클라이언트 메시지 ID는 120자 이하여야 합니다.")
		String clientMessageId,

		MessageType messageType,

		@NotNull(message = "메시지 본문은 필수입니다.")
		JsonNode body,

		UUID resourceId
) {
	public SendChatMessageCommand toCommand() {
		return new SendChatMessageCommand(clientMessageId, messageType, body, resourceId);
	}
}
