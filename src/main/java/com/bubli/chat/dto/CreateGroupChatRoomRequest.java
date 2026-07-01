package com.bubli.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateGroupChatRoomRequest(
		@NotBlank(message = "채팅방 이름은 필수입니다.")
		@Size(max = 120, message = "채팅방 이름은 120자 이하여야 합니다.")
		String name,

		@NotNull(message = "초대할 멤버 목록은 필수입니다.")
		@Size(min = 1, message = "최소 1명 이상의 멤버를 초대해야 합니다.")
		List<UUID> memberUserIds
) {
}
