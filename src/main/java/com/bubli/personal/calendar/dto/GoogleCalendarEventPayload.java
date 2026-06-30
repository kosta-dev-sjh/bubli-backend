package com.bubli.personal.calendar.dto;

import java.time.Instant;

public record GoogleCalendarEventPayload(
		String id,
		String summary,
		EventDateTime start,
		EventDateTime end
) {
	public static GoogleCalendarEventPayload from(String title, Instant startsAt, Instant endsAt) {
		return new GoogleCalendarEventPayload(
				null,
				title,
				new EventDateTime(startsAt.toString()),
				endsAt == null ? null : new EventDateTime(endsAt.toString())
		);
	}

	public record EventDateTime(
			String dateTime
	) {
	}
}
