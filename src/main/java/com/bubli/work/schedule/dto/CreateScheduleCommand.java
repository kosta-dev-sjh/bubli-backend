package com.bubli.work.schedule.dto;

import java.time.Instant;
import java.util.UUID;

public record CreateScheduleCommand(
		UUID roomId,
		UUID taskId,
		UUID wbsItemId,
		String title,
		Instant startsAt,
		Instant endsAt,
		boolean allDay
) {
}
