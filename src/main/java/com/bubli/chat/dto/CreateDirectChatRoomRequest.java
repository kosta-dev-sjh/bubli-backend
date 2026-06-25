package com.bubli.chat.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateDirectChatRoomRequest(
		@NotNull(message = "상대 사용자 ID는 필수입니다.")
		UUID targetUserId
) {
}
