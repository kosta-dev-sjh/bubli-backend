package com.bubli.personal.calendar.dto;

import com.bubli.personal.calendar.type.CalendarEventSourceType;
import com.bubli.work.schedule.dto.ScheduleResult;
import com.bubli.work.schedule.type.ScheduleSyncStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

public record CalendarGroupEventResponse(
		UUID scheduleId,
		UUID ownerUserId,
		UUID roomId,
		String googleCalendarId,
		String googleCalendarSummary,
		String googleEventId,
		String title,
		Instant startsAt,
		Instant endsAt,
		boolean allDay,
		ScheduleSyncStatus syncStatus,
		CalendarEventSourceType sourceType
) {
	private static final String UNTITLED_EVENT = "(제목 없음)";

	public static CalendarGroupEventResponse fromSchedule(ScheduleResult schedule) {
		return new CalendarGroupEventResponse(
				schedule.id(),
				schedule.ownerUserId(),
				schedule.roomId(),
				schedule.googleCalendarId(),
				schedule.googleCalendarSummary(),
				schedule.googleEventId(),
				schedule.title(),
				schedule.startsAt(),
				schedule.endsAt(),
				schedule.allDay(),
				schedule.syncStatus(),
				CalendarEventSourceType.BUBLI
		);
	}

	public static CalendarGroupEventResponse fromGoogleEvent(
			String googleCalendarId,
			String googleCalendarSummary,
			GoogleCalendarEventPayload event
	) {
		EventTimeRange range = EventTimeRange.from(event);
		return new CalendarGroupEventResponse(
				null,
				null,
				null,
				googleCalendarId,
				googleCalendarSummary,
				event.id(),
				normalizeTitle(event.summary()),
				range.startsAt(),
				range.endsAt(),
				range.allDay(),
				ScheduleSyncStatus.SYNCED,
				CalendarEventSourceType.GOOGLE
		);
	}

	private static String normalizeTitle(String title) {
		return title == null || title.isBlank() ? UNTITLED_EVENT : title.trim();
	}

	private record EventTimeRange(Instant startsAt, Instant endsAt, boolean allDay) {
		private static EventTimeRange from(GoogleCalendarEventPayload event) {
			GoogleCalendarEventPayload.EventDateTime start = event.start();
			GoogleCalendarEventPayload.EventDateTime end = event.end();
			if (start != null && start.dateTime() != null) {
				return new EventTimeRange(
						Instant.parse(start.dateTime()),
						end == null || end.dateTime() == null ? null : Instant.parse(end.dateTime()),
						false
				);
			}
			if (start != null && start.date() != null) {
				Instant startsAt = LocalDate.parse(start.date()).atStartOfDay().toInstant(ZoneOffset.UTC);
				Instant endsAt = end == null || end.date() == null
						? null
						: LocalDate.parse(end.date()).atStartOfDay().toInstant(ZoneOffset.UTC);
				return new EventTimeRange(startsAt, endsAt, true);
			}
			throw new IllegalArgumentException("Google Calendar event start time is required.");
		}
	}
}
