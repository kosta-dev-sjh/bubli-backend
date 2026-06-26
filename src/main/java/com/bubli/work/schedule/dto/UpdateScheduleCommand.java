package com.bubli.work.schedule.dto;

import java.time.Instant;
import java.util.UUID;

public record UpdateScheduleCommand(
		String title,
		Instant startsAt,
		Instant endsAt,
		Boolean allDay,
		UUID taskId,
		UUID wbsItemId
) {
}
