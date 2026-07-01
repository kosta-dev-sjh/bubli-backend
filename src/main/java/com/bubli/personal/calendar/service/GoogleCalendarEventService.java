package com.bubli.personal.calendar.service;

import com.bubli.global.response.PageResponse;
import com.bubli.personal.calendar.dto.GoogleCalendarEventPayload;
import com.bubli.personal.calendar.entity.GoogleCalendarConnection;
import com.bubli.work.schedule.dto.CreateScheduleCommand;
import com.bubli.work.schedule.dto.ScheduleResult;
import com.bubli.work.schedule.dto.UpdateScheduleCommand;
import com.bubli.work.schedule.service.ScheduleCalendarPublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GoogleCalendarEventService {

	private final ScheduleCalendarPublicService scheduleCalendarPublicService;
	private final GoogleCalendarConnectionService connectionService;
	private final GoogleCalendarClient googleCalendarClient;

	@Transactional(readOnly = true)
	public PageResponse<ScheduleResult> getEvents(
			UUID userId,
			UUID roomId,
			Instant from,
			Instant to,
			Pageable pageable
	) {
		return scheduleCalendarPublicService.getEvents(userId, roomId, from, to, pageable);
	}

	@Transactional
	public ScheduleResult createEvent(UUID userId, CreateScheduleCommand command) {
		return scheduleCalendarPublicService.createEvent(userId, command);
	}

	@Transactional
	public ScheduleResult updateEvent(UUID userId, UUID scheduleId, UpdateScheduleCommand command) {
		return scheduleCalendarPublicService.updateEvent(userId, scheduleId, command);
	}

	@Transactional
	public void deleteEvent(UUID userId, UUID scheduleId) {
		scheduleCalendarPublicService.deleteEvent(userId, scheduleId);
	}

	@Transactional
	public List<ScheduleResult> syncEvents(UUID userId, Instant from, Instant to) {
		GoogleCalendarConnection connection = connectionService.getActiveConnectionWithFreshToken(userId)
				.orElseThrow(() -> new com.bubli.global.error.BusinessException(
						com.bubli.global.error.ErrorCode.CALENDAR_404_001
				));
		List<GoogleCalendarEventPayload> events = googleCalendarClient.getEvents(
				connection.getAccessToken(),
				from.toString(),
				to.toString()
		);
		return events.stream()
				.filter(event -> event.id() != null && event.start() != null && event.start().dateTime() != null)
				.map(event -> upsertSyncedEvent(userId, event))
				.toList();
	}

	@Transactional
	public List<ScheduleResult> pushUnsyncedEvents(UUID userId, Instant from, Instant to) {
		if (connectionService.getActiveConnectionWithFreshToken(userId).isEmpty()) {
			throw new com.bubli.global.error.BusinessException(
					com.bubli.global.error.ErrorCode.CALENDAR_404_001
			);
		}
		return scheduleCalendarPublicService.pushUnsyncedEvents(userId, from, to);
	}

	@Transactional(readOnly = true)
	public boolean hasActiveConnection(UUID userId) {
		return connectionService.hasActiveConnection(userId);
	}

	private ScheduleResult upsertSyncedEvent(UUID userId, GoogleCalendarEventPayload event) {
		Instant startsAt = Instant.parse(event.start().dateTime());
		Instant endsAt = event.end() == null || event.end().dateTime() == null
				? null
				: Instant.parse(event.end().dateTime());
		return scheduleCalendarPublicService.upsertGoogleEvent(
				userId,
				event.id(),
				event.summary(),
				startsAt,
				endsAt
		);
	}
}
