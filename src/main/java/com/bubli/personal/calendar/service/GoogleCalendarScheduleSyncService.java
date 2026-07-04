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
		try {
			Optional<GoogleCalendarConnection> connection = connectionService.getActiveConnectionWithFreshToken(userId);
			if (connection.isEmpty()) {
				deleteRequestService.rememberFailedAttempt(userId, schedule.googleEventId());
				return;
			}
			googleCalendarClient.deleteEvent(
					connection.get().getAccessToken(),
					schedule.googleCalendarId(),
					schedule.googleEventId()
			);
			deleteRequestService.markSucceeded(userId, schedule.googleEventId());
		} catch (BusinessException exception) {
			// 외부 캘린더 삭제 실패가 Bubli 일정/WBS 삭제를 막지 않게 한다.
			deleteRequestService.rememberFailedAttempt(userId, schedule.googleEventId());
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
	 * 새 이벤트를 생성할 때 룸 일정(WBS 포함)은 룸 이름으로 생성된 전용 Google Calendar로 라우팅한다.
	 * 이미 Google에 존재하는 이벤트는 기존 캘린더 위치를 유지하고, 개인 일정은 primary를 사용한다.
	 */
	private CalendarTarget resolveCalendarTarget(UUID userId, ScheduleSyncTarget schedule) {
		if (schedule.googleEventId() != null && !schedule.googleEventId().isBlank()) {
			return new CalendarTarget(normalizeCalendarId(schedule.googleCalendarId()), schedule.googleCalendarSummary());
		}
		if (schedule.roomId() != null) {
			return projectRoomCalendarService.ensureRoomCalendar(userId, schedule.roomId())
					.map(mapping -> new CalendarTarget(mapping.getGoogleCalendarId(), mapping.getCalendarName()))
					.orElseGet(() -> new CalendarTarget("primary", null));
		}
		return new CalendarTarget("primary", null);
	}

	private String normalizeCalendarId(String calendarId) {
		return calendarId == null || calendarId.isBlank() ? "primary" : calendarId;
	}

	private record CalendarTarget(String calendarId, String calendarSummary) {
	}
}
