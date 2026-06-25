package com.bubli.project.dto;

import com.bubli.project.type.RoomMemberRole;

import java.time.Instant;
import java.util.UUID;

public record CreateInvitationCommand(
		UUID inviteeUserId,
		RoomMemberRole role,
		Instant expiresAt
) {
}
