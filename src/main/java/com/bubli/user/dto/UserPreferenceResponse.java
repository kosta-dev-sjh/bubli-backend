package com.bubli.user.dto;

import java.time.Instant;
import java.util.UUID;

public record UserPreferenceResponse(
		UUID userId,
		String theme,
		String defaultHomeType,
		UUID defaultProjectRoomId,
		Instant createdAt,
		Instant updatedAt
) {
	public static UserPreferenceResponse from(UserPreferenceResult result) {
		return new UserPreferenceResponse(
				result.userId(),
				result.theme(),
				result.defaultHomeType(),
				result.defaultProjectRoomId(),
				result.createdAt(),
				result.updatedAt()
		);
	}
}
