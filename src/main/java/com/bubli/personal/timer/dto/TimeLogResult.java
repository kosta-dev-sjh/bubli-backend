package com.bubli.personal.timer.dto;

import com.bubli.personal.timer.entity.TimeLog;
import com.bubli.personal.timer.type.TimeLogStatus;
import com.bubli.personal.timer.type.TimerType;

import java.time.Instant;
import java.util.UUID;

public record TimeLogResult(
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
	public static TimeLogResult from(TimeLog timeLog) {
		return new TimeLogResult(
				timeLog.getId(),
				timeLog.getUserId(),
				timeLog.getRoomId(),
				timeLog.getTimerType(),
				timeLog.getIdempotencyKey(),
				timeLog.getRecoveredFromTimeLogId(),
				timeLog.getStatus(),
				timeLog.getStartedAt(),
				timeLog.getLastStartedAt(),
				timeLog.getEndedAt(),
				timeLog.getDurationSeconds(),
				timeLog.getLastHeartbeatAt(),
				timeLog.getCreatedAt(),
				timeLog.getUpdatedAt()
		);
	}
}
