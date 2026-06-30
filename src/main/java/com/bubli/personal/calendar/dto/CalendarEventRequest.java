package com.bubli.personal.calendar.dto;

import com.bubli.work.schedule.dto.CreateScheduleCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record CalendarEventRequest(
		UUID roomId,
		UUID taskId,
		UUID wbsItemId,

		@NotBlank(message = "일정 제목은 필수입니다.")
		@Size(max = 200, message = "일정 제목은 200자 이하여야 합니다.")
		String title,

		@NotNull(message = "일정 시작 시각은 필수입니다.")
		Instant startsAt,

		Instant endsAt,
		Boolean allDay
) {
	public CreateScheduleCommand toCommand() {
		return new CreateScheduleCommand(
				roomId,
				taskId,
				wbsItemId,
				title == null ? null : title.trim(),
				startsAt,
				endsAt,
				Boolean.TRUE.equals(allDay)
		);
	}
}
