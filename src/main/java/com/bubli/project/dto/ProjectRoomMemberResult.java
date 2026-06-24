package com.bubli.project.dto;

import com.bubli.project.entity.RoomMember;
import com.bubli.project.type.RoomMemberRole;
import com.bubli.project.type.RoomMemberStatus;
import com.bubli.user.entity.User;

import java.time.Instant;
import java.util.UUID;

public record ProjectRoomMemberResult(
		UUID id,
		UUID roomId,
		UUID userId,
		String bubliId,
		String name,
		String avatarUrl,
		RoomMemberRole role,
		RoomMemberStatus status,
		Instant createdAt,
		Instant updatedAt
) {
	public static ProjectRoomMemberResult from(RoomMember roomMember, User user) {
		return new ProjectRoomMemberResult(
				roomMember.getId(),
				roomMember.getRoomId(),
				roomMember.getUserId(),
				user == null ? null : user.getBubliId(),
				user == null ? null : user.getName(),
				user == null ? null : user.getAvatarUrl(),
				roomMember.getRole(),
				roomMember.getStatus(),
				roomMember.getCreatedAt(),
				roomMember.getUpdatedAt()
		);
	}
}
