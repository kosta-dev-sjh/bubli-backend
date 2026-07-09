package com.bubli.personal.calendar.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Duration;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GoogleCalendarEventPayload(
		String id,
		String status,
		String summary,
		EventDateTime start,
		EventDateTime end
) {
	private static final Duration DEFAULT_TIMED_EVENT_DURATION = Duration.ofMinutes(30);

	public static GoogleCalendarEventPayload from(String title, Instant startsAt, Instant endsAt) {
		return new GoogleCalendarEventPayload(
				null,
				null,
				title,
				new EventDateTime(startsAt.toString()),
				new EventDateTime(normalizeEnd(startsAt, endsAt).toString())
		);
	}

	public static Instant normalizeEnd(Instant startsAt, Instant endsAt) {
		if (endsAt != null) {
			return endsAt;
		}
		return startsAt.plus(DEFAULT_TIMED_EVENT_DURATION);
	}

	public boolean isCancelled() {
		return "cancelled".equalsIgnoreCase(status);
	}

	public record EventDateTime(
			String dateTime,
			String date,
			String timeZone
	) {
		public EventDateTime(String dateTime) {
			this(dateTime, null, null);
		}
	}
}
