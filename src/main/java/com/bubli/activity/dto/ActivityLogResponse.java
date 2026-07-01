package com.bubli.activity.dto;

import java.time.Instant;
import java.util.UUID;

public record ActivityLogResponse(
		UUID id,
		UUID userId,
		UUID roomId,
		String appName,
		String windowTitle,
		Instant startedAt,
		Instant endedAt,
		Long durationSeconds,
		Instant createdAt
) {

	public static ActivityLogResponse from(ActivityLogResult result) {
		return new ActivityLogResponse(
				result.id(),
				result.userId(),
				result.roomId(),
				result.appName(),
				result.windowTitle(),
				result.startedAt(),
				result.endedAt(),
				result.durationSeconds(),
				result.createdAt()
		);
	}
}
