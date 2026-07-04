package com.bubli.personal.calendar.dto;

public record RoomCalendarResponse(
		String googleCalendarId,
		String calendarName,
		boolean connected
) {
}
