package com.bubli.project.dto;

import java.util.UUID;

public record ProjectRoomEventActorResponse(
		String type,
		UUID id,
		String name
) {
	public static ProjectRoomEventActorResponse user(UUID id, String name) {
		return new ProjectRoomEventActorResponse("USER", id, name);
	}

	public static ProjectRoomEventActorResponse system() {
		return new ProjectRoomEventActorResponse("SYSTEM", null, "System");
	}
}
