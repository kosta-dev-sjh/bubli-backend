package com.bubli.personal.calendar.dto;

import com.bubli.personal.calendar.entity.GoogleCalendarConnection;
import com.bubli.personal.calendar.type.GoogleCalendarConnectionStatus;

import java.time.Instant;

public record GoogleCalendarConnectionResponse(
		String googleAccountEmail,
		GoogleCalendarConnectionStatus status,
		Instant expiresAt,
		Instant updatedAt
) {
	public static GoogleCalendarConnectionResponse from(GoogleCalendarConnection connection) {
		return new GoogleCalendarConnectionResponse(
				connection.getGoogleAccountEmail(),
				connection.getStatus(),
				connection.getExpiresAt(),
				connection.getUpdatedAt()
		);
	}
}
