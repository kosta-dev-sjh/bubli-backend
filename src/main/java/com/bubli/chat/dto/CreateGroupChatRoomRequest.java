package com.bubli.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateGroupChatRoomRequest(
		@NotBlank
		@Size(max = 120)
		String name,

		@NotEmpty
		List<UUID> memberUserIds
) {
	public CreateGroupChatRoomRequest {
		memberUserIds = memberUserIds == null ? List.of() : List.copyOf(memberUserIds);
	}
}
