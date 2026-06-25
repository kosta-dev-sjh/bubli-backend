package com.bubli.work.schedule.dto;

import com.bubli.work.schedule.entity.Schedule;
import com.bubli.work.schedule.type.ScheduleSyncStatus;

import java.time.Instant;
import java.util.UUID;

public record ScheduleResult(
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
	public static ScheduleResult from(Schedule schedule) {
		return new ScheduleResult(
				schedule.getId(),
				schedule.getOwnerUserId(),
				schedule.getRoomId(),
				schedule.getTaskId(),
				schedule.getWbsItemId(),
				schedule.getGoogleEventId(),
				schedule.getTitle(),
				schedule.getStartsAt(),
				schedule.getEndsAt(),
				schedule.isAllDay(),
				schedule.getSyncStatus(),
				schedule.getLastSyncedAt(),
				schedule.getCreatedAt(),
				schedule.getUpdatedAt()
		);
	}
}
