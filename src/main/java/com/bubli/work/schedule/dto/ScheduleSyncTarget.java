package com.bubli.work.schedule.dto;

import com.bubli.work.schedule.entity.Schedule;

import java.time.Instant;

public record ScheduleSyncTarget(
		String googleEventId,
		String title,
		Instant startsAt,
		Instant endsAt
) {
	public static ScheduleSyncTarget from(Schedule schedule) {
		return new ScheduleSyncTarget(
				schedule.getGoogleEventId(),
				schedule.getTitle(),
				schedule.getStartsAt(),
				schedule.getEndsAt()
		);
	}
}
