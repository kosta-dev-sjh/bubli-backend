package com.bubli.chat.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record InviteChatRoomMembersRequest(
		@NotEmpty
		List<UUID> memberUserIds
) {
	public InviteChatRoomMembersRequest {
		memberUserIds = memberUserIds == null ? List.of() : List.copyOf(memberUserIds);
	}
}
