package com.bubli.personal.calendar.service;

import com.bubli.global.error.BusinessException;
import com.bubli.personal.calendar.dto.GoogleCalendarEventPayload;
import com.bubli.personal.calendar.dto.GoogleCalendarSyncResult;
import com.bubli.personal.calendar.entity.GoogleCalendarConnection;
import com.bubli.work.schedule.dto.ScheduleSyncTarget;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GoogleCalendarScheduleSyncService implements GoogleCalendarScheduleSyncPublicService {

	private final GoogleCalendarConnectionService connectionService;
	private final GoogleCalendarClient googleCalendarClient;

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
		connectionService.getActiveConnectionWithFreshToken(userId)
				.ifPresent(connection -> googleCalendarClient.deleteEvent(
						connection.getAccessToken(),
						schedule.googleEventId()
				));
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
					: googleCalendarClient.updateEvent(connection.getAccessToken(), schedule.googleEventId(), payload);
			if (synced == null || synced.id() == null || synced.id().isBlank()) {
				return GoogleCalendarSyncResult.failed();
			}
			return GoogleCalendarSyncResult.succeeded(synced.id());
		} catch (BusinessException exception) {
			return GoogleCalendarSyncResult.failed();
		}
	}
}
