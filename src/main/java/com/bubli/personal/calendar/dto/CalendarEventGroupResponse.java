package com.bubli.personal.calendar.dto;

import com.bubli.personal.calendar.type.CalendarEventGroupType;

import java.util.List;
import java.util.UUID;

public record CalendarEventGroupResponse(
		CalendarEventGroupType groupType,
		String groupId,
		String groupName,
		UUID roomId,
		String googleCalendarId,
		int eventCount,
		List<CalendarGroupEventResponse> events
) {
	public static CalendarEventGroupResponse of(
			CalendarEventGroupType groupType,
			String groupId,
			String groupName,
			UUID roomId,
			String googleCalendarId,
			List<CalendarGroupEventResponse> events
	) {
		return new CalendarEventGroupResponse(
				groupType,
				groupId,
				groupName,
				roomId,
				googleCalendarId,
				events.size(),
				events
		);
	}
}
