package com.bubli.project.dto;

import com.bubli.project.entity.Invitation;
import com.bubli.project.entity.ProjectRoom;
import com.bubli.project.type.InvitationStatus;
import com.bubli.project.type.RoomMemberRole;
import com.bubli.user.dto.UserResult;

import java.time.Instant;
import java.util.UUID;

public record InvitationResult(
		UUID id,
		UUID roomId,
		String roomName,
		UUID inviterUserId,
		String inviterBubliId,
		String inviterName,
		String inviterAvatarUrl,
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
		return from(invitation, null, null, invitee);
	}

	public static InvitationResult from(Invitation invitation, ProjectRoom room, UserResult inviter, UserResult invitee) {
		return new InvitationResult(
				invitation.getId(),
				invitation.getRoomId(),
				room == null ? null : room.getName(),
				invitation.getInviterUserId(),
				inviter == null ? null : inviter.bubliId(),
				inviter == null ? null : inviter.name(),
				inviter == null ? null : inviter.avatarUrl(),
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
