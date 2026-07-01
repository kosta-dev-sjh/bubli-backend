package com.bubli.activity.dto;

import com.bubli.activity.entity.ActivityLog;

import java.time.Instant;
import java.util.UUID;

public record ActivityLogResult(
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

	public static ActivityLogResult from(ActivityLog activityLog) {
		return new ActivityLogResult(
				activityLog.getId(),
				activityLog.getUserId(),
				activityLog.getRoomId(),
				activityLog.getAppName(),
				activityLog.getWindowTitle(),
				activityLog.getStartedAt(),
				activityLog.getEndedAt(),
				activityLog.getDurationSeconds(),
				activityLog.getCreatedAt()
		);
	}
}
