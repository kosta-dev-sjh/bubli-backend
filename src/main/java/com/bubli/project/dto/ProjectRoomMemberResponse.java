package com.bubli.project.dto;

import com.bubli.project.type.RoomMemberRole;
import com.bubli.project.type.RoomMemberStatus;

import java.time.Instant;
import java.util.UUID;

public record ProjectRoomMemberResponse(
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
	public static ProjectRoomMemberResponse from(ProjectRoomMemberResult result) {
		return new ProjectRoomMemberResponse(
				result.id(),
				result.roomId(),
				result.userId(),
				result.bubliId(),
				result.name(),
				result.avatarUrl(),
				result.role(),
				result.status(),
				result.createdAt(),
				result.updatedAt()
		);
	}
}
