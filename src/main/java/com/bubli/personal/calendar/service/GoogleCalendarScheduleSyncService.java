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

	@Override
	public GoogleCalendarSyncResult syncCreatedOrUpdatedSchedule(UUID userId, ScheduleSyncTarget schedule) {
		return connectionService.getActiveConnectionWithFreshToken(userId)
				.map(connection -> syncToGoogle(connection, schedule))
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

	private GoogleCalendarSyncResult syncToGoogle(GoogleCalendarConnection connection, ScheduleSyncTarget schedule) {
		try {
			GoogleCalendarEventPayload payload = GoogleCalendarEventPayload.from(
					schedule.title(),
					schedule.startsAt(),
					schedule.endsAt()
			);
			GoogleCalendarEventPayload synced = schedule.googleEventId() == null
					? googleCalendarClient.createEvent(connection.getAccessToken(), payload)
					: googleCalendarClient.updateEvent(
							connection.getAccessToken(),
							schedule.googleCalendarId(),
							schedule.googleEventId(),
							payload
					);
			if (synced == null || synced.id() == null || synced.id().isBlank()) {
				return GoogleCalendarSyncResult.failed();
			}
			return GoogleCalendarSyncResult.succeeded(synced.id());
		} catch (BusinessException exception) {
			return GoogleCalendarSyncResult.failed();
		}
	}
}
