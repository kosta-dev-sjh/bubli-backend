package com.bubli.personal.calendar.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.global.response.PageResponse;
import com.bubli.personal.calendar.dto.GoogleCalendarEventPayload;
import com.bubli.personal.calendar.dto.GoogleCalendarListEntry;
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
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GoogleCalendarEventService {

	private final ScheduleCalendarPublicService scheduleCalendarPublicService;
	private final GoogleCalendarConnectionService connectionService;
	private final GoogleCalendarClient googleCalendarClient;
	private final GoogleCalendarDeleteRequestService deleteRequestService;
	private final ProjectRoomCalendarService projectRoomCalendarService;

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
	public ScheduleResult updateGoogleEvent(
			UUID userId,
			String googleCalendarId,
			String googleEventId,
			UpdateScheduleCommand command
	) {
		validateGoogleEventReference(googleCalendarId, googleEventId);
		validateGoogleEventUpdate(command);
		GoogleCalendarConnection connection = connectionService.getActiveConnectionWithFreshToken(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.CALENDAR_404_001));
		String normalizedCalendarId = normalizeCalendarId(googleCalendarId);
		GoogleCalendarEventPayload updated = googleCalendarClient.updateEvent(
				connection.getAccessToken(),
				normalizedCalendarId,
				googleEventId,
				patchPayload(command)
		);
		EventTimeRange range = hasStartTime(updated) ? EventTimeRange.from(updated) : new EventTimeRange(
				command.startsAt(),
				command.endsAt(),
				Boolean.TRUE.equals(command.allDay())
		);
		if (range.startsAt() == null) {
			throw new BusinessException(ErrorCode.CALENDAR_400_001);
		}
		String title = updated == null || updated.summary() == null || updated.summary().isBlank()
				? command.title()
				: updated.summary();
		return scheduleCalendarPublicService.upsertGoogleEvent(
				userId,
				normalizedCalendarId,
				null,
				googleEventId,
				title,
				range.startsAt(),
				range.endsAt(),
				range.allDay()
		);
	}

	@Transactional
	public void deleteEvent(UUID userId, UUID scheduleId) {
		scheduleCalendarPublicService.deleteEvent(userId, scheduleId);
	}

	@Transactional
	public void deleteGoogleEvent(UUID userId, String googleCalendarId, String googleEventId) {
		validateGoogleEventReference(googleCalendarId, googleEventId);
		GoogleCalendarConnection connection = connectionService.getActiveConnectionWithFreshToken(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.CALENDAR_404_001));
		String normalizedCalendarId = normalizeCalendarId(googleCalendarId);
		googleCalendarClient.deleteEvent(connection.getAccessToken(), normalizedCalendarId, googleEventId);
		scheduleCalendarPublicService.deleteGoogleEventSchedules(userId, normalizedCalendarId, List.of(googleEventId));
		deleteRequestService.markSucceeded(userId, normalizedCalendarId, googleEventId);
	}

	@Transactional
	public List<ScheduleResult> syncEvents(UUID userId, Instant from, Instant to) {
		return syncEvents(userId, from, to, List.of("primary"));
	}

	@Transactional
	public List<ScheduleResult> syncEvents(UUID userId, Instant from, Instant to, List<String> calendarIds) {
		GoogleCalendarConnection connection = connectionService.getActiveConnectionWithFreshToken(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.CALENDAR_404_001));
		List<GoogleCalendarListEntry> calendars = resolveCalendars(connection.getAccessToken(), calendarIds);
		Set<String> managedRoomCalendarIds = projectRoomCalendarService.findManagedGoogleCalendarIds(userId);
		List<ScheduleResult> results = new ArrayList<>();
		for (GoogleCalendarListEntry calendar : calendars.stream()
				.filter(calendar -> !managedRoomCalendarIds.contains(calendar.id()))
				.toList()) {
			results.addAll(syncCalendarEvents(userId, connection.getAccessToken(), calendar, from, to));
		}
		return results;
	}

	@Transactional
	public List<GoogleCalendarListEntry> getGoogleCalendars(UUID userId) {
		GoogleCalendarConnection connection = connectionService.getActiveConnectionWithFreshToken(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.CALENDAR_404_001));
		return googleCalendarClient.getCalendars(connection.getAccessToken());
	}

	private List<ScheduleResult> syncCalendarEvents(
			UUID userId,
			String accessToken,
			GoogleCalendarListEntry calendar,
			Instant from,
			Instant to
	) {
		List<GoogleCalendarEventPayload> events = googleCalendarClient.getEvents(
				accessToken,
				calendar.id(),
				from.toString(),
				to.toString()
		);
		List<String> cancelledIds = events.stream()
				.filter(GoogleCalendarEventPayload::isCancelled)
				.map(GoogleCalendarEventPayload::id)
				.toList();
		scheduleCalendarPublicService.deleteGoogleEventSchedules(
				userId,
				calendar.id(),
				cancelledIds
		);
		deleteRequestService.markSucceeded(userId, calendar.id(), cancelledIds);
		List<GoogleCalendarEventPayload> activeEvents = events.stream()
				.filter(event -> !event.isCancelled())
				.filter(event -> event.id() != null && hasStartTime(event))
				.toList();
		Set<String> pendingDeleteIds = deleteRequestService.findPendingGoogleEventIds(
				userId,
				calendar.id(),
				activeEvents.stream()
						.map(GoogleCalendarEventPayload::id)
						.toList()
		);
		List<ScheduleResult> results = new ArrayList<>();
		for (GoogleCalendarEventPayload event : activeEvents) {
			if (pendingDeleteIds.contains(event.id())) {
				retryPendingDelete(userId, accessToken, calendar.id(), event.id());
				continue;
			}
			results.add(upsertSyncedEvent(userId, calendar, event));
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

	private ScheduleResult upsertSyncedEvent(UUID userId, GoogleCalendarListEntry calendar, GoogleCalendarEventPayload event) {
		EventTimeRange range = EventTimeRange.from(event);
		return scheduleCalendarPublicService.upsertGoogleEvent(
				userId,
				calendar.id(),
				calendar.displayName(),
				event.id(),
				event.summary(),
				range.startsAt(),
				range.endsAt(),
				range.allDay()
		);
	}

	private void retryPendingDelete(UUID userId, String accessToken, String googleCalendarId, String googleEventId) {
		try {
			googleCalendarClient.deleteEvent(accessToken, googleCalendarId, googleEventId);
			deleteRequestService.markSucceeded(userId, googleCalendarId, googleEventId);
		} catch (BusinessException exception) {
			deleteRequestService.rememberFailedAttempt(userId, googleCalendarId, googleEventId);
		}
	}

	private List<GoogleCalendarListEntry> resolveCalendars(String accessToken, List<String> calendarIds) {
		if (calendarIds == null || calendarIds.isEmpty()) {
			List<GoogleCalendarListEntry> calendars = googleCalendarClient.getCalendars(accessToken);
			return calendars.isEmpty()
					? List.of(new GoogleCalendarListEntry("primary", "Primary", true, null, true, null))
					: calendars.stream()
							.filter(calendar -> !Boolean.FALSE.equals(calendar.selected()))
							.toList();
		}

		List<String> normalizedIds = calendarIds.stream()
				.filter(id -> id != null && !id.isBlank())
				.distinct()
				.toList();
		if (normalizedIds.isEmpty() || normalizedIds.equals(List.of("primary"))) {
			return List.of(new GoogleCalendarListEntry("primary", "Primary", true, null, true, null));
		}
		List<GoogleCalendarListEntry> calendars = googleCalendarClient.getCalendars(accessToken);
		Map<String, GoogleCalendarListEntry> resolved = new LinkedHashMap<>();
		if (normalizedIds.contains("primary")) {
			resolved.put("primary", new GoogleCalendarListEntry("primary", "Primary", true, null, true, null));
		}
		calendars.stream()
				.filter(calendar -> normalizedIds.contains(calendar.id()))
				.forEach(calendar -> resolved.put(calendar.id(), calendar));
		return List.copyOf(resolved.values());
	}

	private void validateGoogleEventReference(String googleCalendarId, String googleEventId) {
		if (googleCalendarId == null || googleCalendarId.isBlank()
				|| googleEventId == null || googleEventId.isBlank()) {
			throw new BusinessException(ErrorCode.CALENDAR_400_001);
		}
	}

	private void validateGoogleEventUpdate(UpdateScheduleCommand command) {
		if (command == null) {
			throw new BusinessException(ErrorCode.CALENDAR_400_001);
		}
		boolean empty = command.title() == null
				&& command.startsAt() == null
				&& command.endsAt() == null
				&& command.allDay() == null;
		if (empty || command.title() != null && command.title().isBlank()) {
			throw new BusinessException(ErrorCode.CALENDAR_400_001);
		}
		if (command.startsAt() != null && command.endsAt() != null && !command.endsAt().isAfter(command.startsAt())) {
			throw new BusinessException(ErrorCode.CALENDAR_400_001);
		}
	}

	private GoogleCalendarEventPayload patchPayload(UpdateScheduleCommand command) {
		return new GoogleCalendarEventPayload(
				null,
				null,
				command.title(),
				command.startsAt() == null ? null : new GoogleCalendarEventPayload.EventDateTime(command.startsAt().toString()),
				command.endsAt() == null ? null : new GoogleCalendarEventPayload.EventDateTime(command.endsAt().toString())
		);
	}

	private String normalizeCalendarId(String googleCalendarId) {
		return googleCalendarId == null || googleCalendarId.isBlank() ? "primary" : googleCalendarId;
	}

	private boolean hasStartTime(GoogleCalendarEventPayload event) {
		if (event == null || event.start() == null) {
			return false;
		}
		return event.start().dateTime() != null || event.start().date() != null;
	}

	private record EventTimeRange(Instant startsAt, Instant endsAt, boolean allDay) {
		private static EventTimeRange from(GoogleCalendarEventPayload event) {
			GoogleCalendarEventPayload.EventDateTime start = event.start();
			GoogleCalendarEventPayload.EventDateTime end = event.end();
			if (start.dateTime() != null) {
				return new EventTimeRange(
						Instant.parse(start.dateTime()),
						end == null || end.dateTime() == null ? null : Instant.parse(end.dateTime()),
						false
				);
			}
			Instant startsAt = LocalDate.parse(start.date()).atStartOfDay().toInstant(ZoneOffset.UTC);
			Instant endsAt = end == null || end.date() == null
					? null
					: LocalDate.parse(end.date()).atStartOfDay().toInstant(ZoneOffset.UTC);
			return new EventTimeRange(startsAt, endsAt, true);
		}
	}
}
