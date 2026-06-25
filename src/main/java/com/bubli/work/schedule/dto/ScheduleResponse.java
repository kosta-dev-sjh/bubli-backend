package com.bubli.work.schedule.dto;

import com.bubli.work.schedule.type.ScheduleSyncStatus;

import java.time.Instant;
import java.util.UUID;

public record ScheduleResponse(
		UUID id,
		UUID ownerUserId,
		UUID roomId,
		UUID taskId,
		UUID wbsItemId,
		String googleEventId,
		String title,
		Instant startsAt,
		Instant endsAt,
		boolean allDay,
		ScheduleSyncStatus syncStatus,
		Instant lastSyncedAt,
		Instant createdAt,
		Instant updatedAt
) {
	public static ScheduleResponse from(ScheduleResult schedule) {
		return new ScheduleResponse(
				schedule.id(),
				schedule.ownerUserId(),
				schedule.roomId(),
				schedule.taskId(),
				schedule.wbsItemId(),
				schedule.googleEventId(),
				schedule.title(),
				schedule.startsAt(),
				schedule.endsAt(),
				schedule.allDay(),
				schedule.syncStatus(),
				schedule.lastSyncedAt(),
				schedule.createdAt(),
				schedule.updatedAt()
		);
	}
}
