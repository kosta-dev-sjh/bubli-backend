package com.bubli.personal.calendar.dto;

import com.bubli.work.schedule.dto.ScheduleResponse;
import com.bubli.work.schedule.dto.ScheduleResult;
import com.bubli.work.schedule.type.ScheduleSyncStatus;

public record CalendarEventResponse(
		ScheduleResponse schedule,
		boolean calendarConnectionRequired
) {
	public static CalendarEventResponse from(ScheduleResult schedule, boolean calendarConnected) {
		boolean connectionRequired = !calendarConnected && schedule.syncStatus() == ScheduleSyncStatus.LOCAL_ONLY;
		return new CalendarEventResponse(ScheduleResponse.from(schedule), connectionRequired);
	}
}
