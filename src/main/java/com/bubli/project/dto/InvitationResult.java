package com.bubli.project.dto;

import com.bubli.project.entity.Invitation;
import com.bubli.project.type.InvitationStatus;
import com.bubli.project.type.RoomMemberRole;
import com.bubli.user.dto.UserResult;

import java.time.Instant;
import java.util.UUID;

public record InvitationResult(
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
	public static InvitationResult from(Invitation invitation, UserResult invitee) {
		return new InvitationResult(
				invitation.getId(),
				invitation.getRoomId(),
				invitation.getInviterUserId(),
				invitation.getInviteeUserId(),
				invitee == null ? null : invitee.bubliId(),
				invitee == null ? null : invitee.name(),
				invitee == null ? null : invitee.avatarUrl(),
				invitation.getRole(),
				invitation.getStatus(),
				invitation.getExpiresAt(),
				invitation.getAcceptedAt(),
				invitation.getCreatedAt(),
				invitation.getUpdatedAt()
		);
	}
}
