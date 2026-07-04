package com.bubli.personal.calendar.dto;

public record RoomCalendarResponse(
		String googleCalendarId,
		String calendarName,
		boolean connected,
		boolean needsReconsent
) {
	public static RoomCalendarResponse of(String googleCalendarId, String calendarName, boolean connected) {
		return new RoomCalendarResponse(googleCalendarId, calendarName, connected, false);
	}

	public static RoomCalendarResponse reconsentRequired(String calendarName) {
		// 기존 연동 토큰에 calendars.insert 권한이 없어 룸 캘린더 생성이 거부된 경우.
		return new RoomCalendarResponse(null, calendarName, true, true);
	}
}
