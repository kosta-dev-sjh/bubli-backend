package com.bubli.work.wbs.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ApplyWbsToCalendarRequest(
		@NotNull(message = "일정 시작 시각은 필수입니다.")
		Instant startsAt,

		Instant endsAt,

		@Min(value = 1, message = "WBS 항목별 일정 시간은 1분 이상이어야 합니다.")
		@Max(value = 1440, message = "WBS 항목별 일정 시간은 1440분 이하여야 합니다.")
		Integer itemDurationMinutes,

		@Min(value = 0, message = "WBS 항목 사이 간격은 0분 이상이어야 합니다.")
		@Max(value = 1440, message = "WBS 항목 사이 간격은 1440분 이하여야 합니다.")
		Integer gapMinutes,

		Boolean allDay,
		Boolean includeDone,
		List<UUID> wbsItemIds
) {
	public ApplyWbsToCalendarCommand toCommand() {
		return new ApplyWbsToCalendarCommand(
				startsAt,
				endsAt,
				itemDurationMinutes,
				gapMinutes,
				allDay,
				includeDone,
				wbsItemIds
		);
	}
}
