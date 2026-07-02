package com.bubli.work.wbs.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ApplyWbsToCalendarCommand(
		Instant startsAt,
		Instant endsAt,
		Integer itemDurationMinutes,
		Integer gapMinutes,
		Boolean allDay,
		Boolean includeDone,
		List<UUID> wbsItemIds
) {
	public int effectiveItemDurationMinutes() {
		return itemDurationMinutes == null ? 60 : itemDurationMinutes;
	}

	public int effectiveGapMinutes() {
		return gapMinutes == null ? 0 : gapMinutes;
	}

	public boolean effectiveAllDay() {
		return Boolean.TRUE.equals(allDay);
	}

	public boolean effectiveIncludeDone() {
		return includeDone == null || includeDone;
	}
}
