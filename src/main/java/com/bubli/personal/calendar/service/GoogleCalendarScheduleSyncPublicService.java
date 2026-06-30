package com.bubli.personal.calendar.service;

import com.bubli.personal.calendar.dto.GoogleCalendarSyncResult;
import com.bubli.work.schedule.dto.ScheduleSyncTarget;

import java.util.UUID;

public interface GoogleCalendarScheduleSyncPublicService {

	GoogleCalendarSyncResult syncCreatedOrUpdatedSchedule(UUID userId, ScheduleSyncTarget schedule);

	void deleteSyncedSchedule(UUID userId, ScheduleSyncTarget schedule);
}
