package com.bubli.personal.calendar.dto;

public record GoogleCalendarSyncResult(
		boolean attempted,
		boolean succeeded,
		String googleEventId
) {
	public static GoogleCalendarSyncResult skipped() {
		return new GoogleCalendarSyncResult(false, false, null);
	}

	public static GoogleCalendarSyncResult succeeded(String googleEventId) {
		return new GoogleCalendarSyncResult(true, true, googleEventId);
	}

	public static GoogleCalendarSyncResult failed() {
		return new GoogleCalendarSyncResult(true, false, null);
	}
}
