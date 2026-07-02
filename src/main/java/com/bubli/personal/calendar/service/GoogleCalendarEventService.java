package com.bubli.personal.calendar.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GoogleCalendarEventService {

	private final ScheduleCalendarPublicService scheduleCalendarPublicService;
	private final GoogleCalendarConnectionService connectionService;
	private final GoogleCalendarClient googleCalendarClient;
	private final GoogleCalendarDeleteRequestService deleteRequestService;

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
				.orElseThrow(() -> new BusinessException(ErrorCode.CALENDAR_404_001));
		List<GoogleCalendarEventPayload> events = googleCalendarClient.getEvents(
				connection.getAccessToken(),
				from.toString(),
				to.toString()
		);
		List<String> cancelledIds = events.stream()
				.filter(GoogleCalendarEventPayload::isCancelled)
				.map(GoogleCalendarEventPayload::id)
				.toList();
		scheduleCalendarPublicService.deleteGoogleEventSchedules(
				userId,
				cancelledIds
		);
		deleteRequestService.markSucceeded(userId, cancelledIds);
		List<GoogleCalendarEventPayload> activeEvents = events.stream()
				.filter(event -> !event.isCancelled())
				.filter(event -> event.id() != null && event.start() != null && event.start().dateTime() != null)
				.toList();
		Set<String> pendingDeleteIds = deleteRequestService.findPendingGoogleEventIds(
				userId,
				activeEvents.stream()
						.map(GoogleCalendarEventPayload::id)
						.toList()
		);
		List<ScheduleResult> results = new ArrayList<>();
		for (GoogleCalendarEventPayload event : activeEvents) {
			if (pendingDeleteIds.contains(event.id())) {
				retryPendingDelete(userId, connection.getAccessToken(), event.id());
				continue;
			}
			results.add(upsertSyncedEvent(userId, event));
		}
		return results;
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

	private void retryPendingDelete(UUID userId, String accessToken, String googleEventId) {
		try {
			googleCalendarClient.deleteEvent(accessToken, googleEventId);
			deleteRequestService.markSucceeded(userId, googleEventId);
		} catch (BusinessException exception) {
			deleteRequestService.rememberFailedAttempt(userId, googleEventId);
		}
	}
}
