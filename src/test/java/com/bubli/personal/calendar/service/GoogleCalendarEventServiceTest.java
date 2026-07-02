package com.bubli.personal.calendar.service;

import com.bubli.personal.calendar.dto.GoogleCalendarEventPayload;
import com.bubli.personal.calendar.entity.GoogleCalendarConnection;
import com.bubli.work.schedule.dto.ScheduleResult;
import com.bubli.work.schedule.service.ScheduleCalendarPublicService;
import com.bubli.work.schedule.type.ScheduleSyncStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GoogleCalendarEventServiceTest {

	@Mock
	ScheduleCalendarPublicService scheduleCalendarPublicService;

	@Mock
	GoogleCalendarConnectionService connectionService;

	@Mock
	GoogleCalendarClient googleCalendarClient;

	@InjectMocks
	GoogleCalendarEventService googleCalendarEventService;

	@Test
	void syncEventsDeletesCancelledGoogleEventsAndUpsertsActiveEvents() {
		UUID userId = UUID.randomUUID();
		Instant from = Instant.parse("2026-07-01T00:00:00Z");
		Instant to = Instant.parse("2026-08-01T00:00:00Z");
		GoogleCalendarConnection connection = GoogleCalendarConnection.create(
				userId,
				"user@example.com",
				"access-token",
				"refresh-token",
				Instant.parse("2026-07-03T00:00:00Z")
		);
		GoogleCalendarEventPayload cancelled = new GoogleCalendarEventPayload(
				"google-deleted",
				"cancelled",
				null,
				null,
				null
		);
		GoogleCalendarEventPayload active = new GoogleCalendarEventPayload(
				"google-active",
				"confirmed",
				"검토 회의",
				new GoogleCalendarEventPayload.EventDateTime("2026-07-10T01:00:00Z"),
				new GoogleCalendarEventPayload.EventDateTime("2026-07-10T02:00:00Z")
		);
		ScheduleResult synced = new ScheduleResult(
				UUID.randomUUID(),
				userId,
				null,
				null,
				null,
				"google-active",
				"검토 회의",
				Instant.parse("2026-07-10T01:00:00Z"),
				Instant.parse("2026-07-10T02:00:00Z"),
				false,
				ScheduleSyncStatus.SYNCED,
				Instant.parse("2026-07-10T02:00:00Z"),
				Instant.parse("2026-07-10T01:00:00Z"),
				Instant.parse("2026-07-10T01:00:00Z")
		);

		given(connectionService.getActiveConnectionWithFreshToken(userId)).willReturn(Optional.of(connection));
		given(googleCalendarClient.getEvents("access-token", from.toString(), to.toString()))
				.willReturn(List.of(cancelled, active));
		given(scheduleCalendarPublicService.upsertGoogleEvent(
				userId,
				"google-active",
				"검토 회의",
				Instant.parse("2026-07-10T01:00:00Z"),
				Instant.parse("2026-07-10T02:00:00Z")
		)).willReturn(synced);

		List<ScheduleResult> results = googleCalendarEventService.syncEvents(userId, from, to);

		assertThat(results).containsExactly(synced);
		verify(scheduleCalendarPublicService).deleteGoogleEventSchedules(userId, List.of("google-deleted"));
		verify(scheduleCalendarPublicService).upsertGoogleEvent(
				userId,
				"google-active",
				"검토 회의",
				Instant.parse("2026-07-10T01:00:00Z"),
				Instant.parse("2026-07-10T02:00:00Z")
		);
	}

	@Test
	void syncEventsDoesNotUpsertCancelledGoogleEvents() {
		UUID userId = UUID.randomUUID();
		Instant from = Instant.parse("2026-07-01T00:00:00Z");
		Instant to = Instant.parse("2026-08-01T00:00:00Z");
		GoogleCalendarConnection connection = GoogleCalendarConnection.create(
				userId,
				"user@example.com",
				"access-token",
				"refresh-token",
				Instant.parse("2026-07-03T00:00:00Z")
		);
		GoogleCalendarEventPayload cancelled = new GoogleCalendarEventPayload(
				"google-deleted",
				"cancelled",
				null,
				null,
				null
		);

		given(connectionService.getActiveConnectionWithFreshToken(userId)).willReturn(Optional.of(connection));
		given(googleCalendarClient.getEvents("access-token", from.toString(), to.toString()))
				.willReturn(List.of(cancelled));

		List<ScheduleResult> results = googleCalendarEventService.syncEvents(userId, from, to);

		assertThat(results).isEmpty();
		verify(scheduleCalendarPublicService).deleteGoogleEventSchedules(userId, List.of("google-deleted"));
		verify(scheduleCalendarPublicService, never()).upsertGoogleEvent(any(), any(), any(), any(), any());
	}
}
