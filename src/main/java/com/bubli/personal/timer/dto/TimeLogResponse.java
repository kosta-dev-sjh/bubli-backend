package com.bubli.personal.timer.dto;

import com.bubli.personal.timer.type.TimeLogStatus;
import com.bubli.personal.timer.type.TimerType;

import java.time.Instant;
import java.util.UUID;

public record TimeLogResponse(
		UUID id,
		UUID userId,
		UUID roomId,
		TimerType timerType,
		String idempotencyKey,
		UUID recoveredFromTimeLogId,
		TimeLogStatus status,
		Instant startedAt,
		Instant lastStartedAt,
		Instant endedAt,
		Long durationSeconds,
		Instant lastHeartbeatAt,
		Instant createdAt,
		Instant updatedAt
) {
	public static TimeLogResponse from(TimeLogResult result) {
		return new TimeLogResponse(
				result.id(),
				result.userId(),
				result.roomId(),
				result.timerType(),
				result.idempotencyKey(),
				result.recoveredFromTimeLogId(),
				result.status(),
				result.startedAt(),
				result.lastStartedAt(),
				result.endedAt(),
				result.durationSeconds(),
				result.lastHeartbeatAt(),
				result.createdAt(),
				result.updatedAt()
		);
	}
}
