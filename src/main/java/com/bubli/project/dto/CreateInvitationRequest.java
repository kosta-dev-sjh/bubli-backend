package com.bubli.project.dto;

import com.bubli.project.type.RoomMemberRole;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record CreateInvitationRequest(
		@NotNull(message = "초대받을 사용자 ID는 필수입니다.")
		UUID inviteeUserId,

		RoomMemberRole role,
		Instant expiresAt
) {
	public CreateInvitationCommand toCommand() {
		return new CreateInvitationCommand(inviteeUserId, role, expiresAt);
	}
}
