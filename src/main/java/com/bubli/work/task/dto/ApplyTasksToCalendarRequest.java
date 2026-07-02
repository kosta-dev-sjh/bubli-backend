package com.bubli.work.task.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.List;
import java.util.UUID;

public record ApplyTasksToCalendarRequest(
		@Min(value = 1, message = "TODO 일정 시간은 1분 이상이어야 합니다.")
		@Max(value = 1440, message = "TODO 일정 시간은 1440분 이하여야 합니다.")
		Integer taskDurationMinutes,

		Boolean includeDone,
		List<UUID> taskIds
) {
	public ApplyTasksToCalendarCommand toCommand() {
		return new ApplyTasksToCalendarCommand(
				taskDurationMinutes,
				includeDone,
				taskIds
		);
	}
}
