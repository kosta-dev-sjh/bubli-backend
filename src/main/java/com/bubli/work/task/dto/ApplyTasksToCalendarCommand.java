package com.bubli.work.task.dto;

import java.util.List;
import java.util.UUID;

public record ApplyTasksToCalendarCommand(
		Integer taskDurationMinutes,
		Boolean includeDone,
		List<UUID> taskIds
) {
	public int effectiveTaskDurationMinutes() {
		return taskDurationMinutes == null ? 60 : taskDurationMinutes;
	}

	public boolean effectiveIncludeDone() {
		return includeDone == null || includeDone;
	}
}
