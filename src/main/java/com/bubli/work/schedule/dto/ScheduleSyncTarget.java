package com.bubli.work.schedule.dto;

import com.bubli.work.schedule.entity.Schedule;

import java.time.Instant;
import java.util.UUID;

public record ScheduleSyncTarget(
		UUID roomId,
		String googleCalendarId,
		String googleCalendarSummary,
		String googleEventId,
		String title,
		Instant startsAt,
		Instant endsAt
) {
	public static ScheduleSyncTarget from(Schedule schedule) {
		return new ScheduleSyncTarget(
				schedule.getRoomId(),
				schedule.getGoogleCalendarId(),
				schedule.getGoogleCalendarSummary(),
				schedule.getGoogleEventId(),
				schedule.getTitle(),
				schedule.getStartsAt(),
				schedule.getEndsAt()
		);
	}
}
