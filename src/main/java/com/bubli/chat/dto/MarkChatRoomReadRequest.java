package com.bubli.chat.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MarkChatRoomReadRequest(
		@NotNull(message = "읽은 메시지 ID는 필수입니다.")
		UUID lastReadMessageId
) {
}
