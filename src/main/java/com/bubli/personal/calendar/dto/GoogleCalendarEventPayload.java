package com.bubli.personal.calendar.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GoogleCalendarEventPayload(
		String id,
		String status,
		String summary,
		EventDateTime start,
		EventDateTime end
) {
	public static GoogleCalendarEventPayload from(String title, Instant startsAt, Instant endsAt) {
		return new GoogleCalendarEventPayload(
				null,
				null,
				title,
				new EventDateTime(startsAt.toString()),
				endsAt == null ? null : new EventDateTime(endsAt.toString())
		);
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
