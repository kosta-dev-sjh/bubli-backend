package com.bubli.chat.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record MarkChatRoomReadRequest(
		@NotNull(message = "읽은 메시지 순서는 필수입니다.")
		@Positive(message = "읽은 메시지 순서는 1 이상이어야 합니다.")
		Long lastReadSequence
) {
}
