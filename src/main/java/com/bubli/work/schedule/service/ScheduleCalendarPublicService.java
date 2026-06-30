package com.bubli.work.schedule.service;

import com.bubli.global.response.PageResponse;
import com.bubli.work.schedule.dto.CreateScheduleCommand;
import com.bubli.work.schedule.dto.ScheduleResult;
import com.bubli.work.schedule.dto.UpdateScheduleCommand;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ScheduleCalendarPublicService {

	PageResponse<ScheduleResult> getEvents(UUID userId, UUID roomId, Instant from, Instant to, Pageable pageable);

	ScheduleResult createEvent(UUID userId, CreateScheduleCommand command);

	ScheduleResult updateEvent(UUID userId, UUID scheduleId, UpdateScheduleCommand command);

	void deleteEvent(UUID userId, UUID scheduleId);

	ScheduleResult upsertGoogleEvent(UUID userId, String googleEventId, String title, Instant startsAt, Instant endsAt);

	List<ScheduleResult> pushUnsyncedEvents(UUID userId, Instant from, Instant to);
}
