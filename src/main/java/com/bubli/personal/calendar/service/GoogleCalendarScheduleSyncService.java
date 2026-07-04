package com.bubli.personal.calendar.service;

import com.bubli.global.error.BusinessException;
import com.bubli.personal.calendar.dto.GoogleCalendarEventPayload;
import com.bubli.personal.calendar.dto.GoogleCalendarSyncResult;
import com.bubli.personal.calendar.entity.GoogleCalendarConnection;
import com.bubli.work.schedule.dto.ScheduleSyncTarget;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GoogleCalendarScheduleSyncService implements GoogleCalendarScheduleSyncPublicService {

	private final GoogleCalendarConnectionService connectionService;
	private final GoogleCalendarClient googleCalendarClient;
	private final GoogleCalendarDeleteRequestService deleteRequestService;
	private final ProjectRoomCalendarService projectRoomCalendarService;

	@Override
	public GoogleCalendarSyncResult syncCreatedOrUpdatedSchedule(UUID userId, ScheduleSyncTarget schedule) {
		return connectionService.getActiveConnectionWithFreshToken(userId)
				.map(connection -> syncToGoogle(userId, connection, schedule))
				.orElseGet(GoogleCalendarSyncResult::skipped);
	}

	@Override
	public void deleteSyncedSchedule(UUID userId, ScheduleSyncTarget schedule) {
		if (schedule.googleEventId() == null || schedule.googleEventId().isBlank()) {
			return;
		}
		String calendarId = normalizeCalendarId(schedule.googleCalendarId());
		try {
			Optional<GoogleCalendarConnection> connection = connectionService.getActiveConnectionWithFreshToken(userId);
			if (connection.isEmpty()) {
				deleteRequestService.rememberFailedAttempt(userId, calendarId, schedule.googleEventId());
				return;
			}
			googleCalendarClient.deleteEvent(
					connection.get().getAccessToken(),
					calendarId,
					schedule.googleEventId()
			);
			deleteRequestService.markSucceeded(userId, calendarId, schedule.googleEventId());
		} catch (BusinessException exception) {
			// 외부 캘린더 삭제 실패가 Bubli 일정/WBS 삭제를 막지 않게 한다.
			deleteRequestService.rememberFailedAttempt(userId, calendarId, schedule.googleEventId());
		}
	}

	private GoogleCalendarSyncResult syncToGoogle(
			UUID userId,
			GoogleCalendarConnection connection,
			ScheduleSyncTarget schedule
	) {
		try {
			GoogleCalendarEventPayload payload = GoogleCalendarEventPayload.from(
					schedule.title(),
					schedule.startsAt(),
					schedule.endsAt()
			);
			CalendarTarget target = resolveCalendarTarget(userId, schedule);
			if (target == null) {
				return GoogleCalendarSyncResult.failed();
			}
			GoogleCalendarEventPayload synced = schedule.googleEventId() == null
					? googleCalendarClient.createEvent(connection.getAccessToken(), target.calendarId(), payload)
					: googleCalendarClient.updateEvent(
							connection.getAccessToken(),
							target.calendarId(),
							schedule.googleEventId(),
							payload
					);
			if (synced == null || synced.id() == null || synced.id().isBlank()) {
				return GoogleCalendarSyncResult.failed();
			}
			return GoogleCalendarSyncResult.succeeded(synced.id(), target.calendarId(), target.calendarSummary());
		} catch (BusinessException exception) {
			return GoogleCalendarSyncResult.failed();
		}
	}

	/**
	 * 새 이벤트를 생성할 때 개인 일정은 기본 캘린더, 프로젝트룸 일정은 프로젝트룸 전용 Google Calendar로 라우팅한다.
	 * 이미 Google에 존재하는 이벤트는 기존 캘린더 위치를 유지한다.
	 */
	private CalendarTarget resolveCalendarTarget(UUID userId, ScheduleSyncTarget schedule) {
		if (schedule.googleEventId() != null && !schedule.googleEventId().isBlank()) {
			return new CalendarTarget(normalizeCalendarId(schedule.googleCalendarId()), schedule.googleCalendarSummary());
		}
		if (schedule.roomId() != null) {
			return projectRoomCalendarService.ensureRoomCalendar(userId, schedule.roomId())
					.map(mapping -> new CalendarTarget(mapping.getGoogleCalendarId(), mapping.getCalendarName()))
					.orElse(null);
		}
		return new CalendarTarget("primary", null);
	}

	private String normalizeCalendarId(String calendarId) {
		return calendarId == null || calendarId.isBlank() ? "primary" : calendarId;
	}

	private record CalendarTarget(String calendarId, String calendarSummary) {
	}
}
