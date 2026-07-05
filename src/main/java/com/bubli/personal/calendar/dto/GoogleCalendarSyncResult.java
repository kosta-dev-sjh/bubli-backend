package com.bubli.personal.calendar.dto;

public record GoogleCalendarSyncResult(
		boolean attempted,
		boolean succeeded,
		String googleEventId,
		String googleCalendarId,
		String googleCalendarSummary
) {
	public static GoogleCalendarSyncResult skipped() {
		return new GoogleCalendarSyncResult(false, false, null, null, null);
	}

	public static GoogleCalendarSyncResult succeeded(String googleEventId) {
		return succeeded(googleEventId, "primary", null);
	}

	public static GoogleCalendarSyncResult succeeded(
			String googleEventId,
			String googleCalendarId,
			String googleCalendarSummary
	) {
		return new GoogleCalendarSyncResult(true, true, googleEventId, googleCalendarId, googleCalendarSummary);
	}

	public static GoogleCalendarSyncResult failed() {
		return new GoogleCalendarSyncResult(true, false, null, null, null);
	}
}
