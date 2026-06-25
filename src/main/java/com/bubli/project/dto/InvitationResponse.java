package com.bubli.project.dto;

import com.bubli.project.type.InvitationStatus;
import com.bubli.project.type.RoomMemberRole;

import java.time.Instant;
import java.util.UUID;

public record InvitationResponse(
		UUID id,
		UUID roomId,
		UUID inviterUserId,
		UUID inviteeUserId,
		String inviteeBubliId,
		String inviteeName,
		String inviteeAvatarUrl,
		RoomMemberRole role,
		InvitationStatus status,
		Instant expiresAt,
		Instant acceptedAt,
		Instant createdAt,
		Instant updatedAt
) {
	public static InvitationResponse from(InvitationResult result) {
		return new InvitationResponse(
				result.id(),
				result.roomId(),
				result.inviterUserId(),
				result.inviteeUserId(),
				result.inviteeBubliId(),
				result.inviteeName(),
				result.inviteeAvatarUrl(),
				result.role(),
				result.status(),
				result.expiresAt(),
				result.acceptedAt(),
				result.createdAt(),
				result.updatedAt()
		);
	}
}
