package com.bubli.work.schedule.dto;

import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record UpdateScheduleRequest(
		@Size(max = 200, message = "일정 제목은 200자 이하여야 합니다.")
		String title,

		Instant startsAt,
		Instant endsAt,
		Boolean allDay,
		UUID taskId,
		UUID wbsItemId
) {
	public UpdateScheduleCommand toCommand() {
		return new UpdateScheduleCommand(
				title == null ? null : title.trim(),
				startsAt,
				endsAt,
				allDay,
				taskId,
				wbsItemId
		);
	}
}
