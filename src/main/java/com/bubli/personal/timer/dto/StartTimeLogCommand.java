package com.bubli.personal.timer.dto;

import com.bubli.personal.timer.type.TimerType;

import java.util.UUID;

public record StartTimeLogCommand(
		UUID userId,
		UUID roomId,
		TimerType timerType,
		String idempotencyKey,
		UUID recoveredFromTimeLogId
) {
	public static StartTimeLogCommand of(UUID userId, StartTimeLogRequest request) {
		return new StartTimeLogCommand(
				userId,
				request.roomId(),
				request.timerType(),
				request.idempotencyKey(),
				request.recoveredFromTimeLogId()
		);
	}
}
