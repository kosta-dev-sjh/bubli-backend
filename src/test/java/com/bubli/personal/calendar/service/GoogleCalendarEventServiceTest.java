package com.bubli.personal.calendar.service;

import com.bubli.personal.calendar.dto.GoogleCalendarEventPayload;
import com.bubli.personal.calendar.entity.GoogleCalendarConnection;
import com.bubli.work.schedule.dto.ScheduleResult;
import com.bubli.work.schedule.dto.UpdateScheduleCommand;
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
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

	@Mock
	GoogleCalendarDeleteRequestService deleteRequestService;

	@InjectMocks
	GoogleCalendarEventService googleCalendarEventService;

	@Test
	void updateGoogleEventPatchesGoogleAndCachesUpdatedSchedule() {
		UUID userId = UUID.randomUUID();
		GoogleCalendarConnection connection = GoogleCalendarConnection.create(
				userId,
				"user@example.com",
				"access-token",
				"refresh-token",
				Instant.parse("2026-07-03T00:00:00Z")
		);
		GoogleCalendarEventPayload updated = new GoogleCalendarEventPayload(
				"google-event-1",
				"confirmed",
				"수정된 일정",
				new GoogleCalendarEventPayload.EventDateTime("2026-07-10T01:00:00Z"),
				new GoogleCalendarEventPayload.EventDateTime("2026-07-10T02:00:00Z")
		);
		ScheduleResult cached = new ScheduleResult(
				UUID.randomUUID(),
				userId,
				null,
				null,
				null,
				"google-event-1",
				"primary",
				null,
				"수정된 일정",
				Instant.parse("2026-07-10T01:00:00Z"),
				Instant.parse("2026-07-10T02:00:00Z"),
				false,
				ScheduleSyncStatus.SYNCED,
				Instant.parse("2026-07-10T02:00:00Z"),
				Instant.parse("2026-07-10T01:00:00Z"),
				Instant.parse("2026-07-10T01:00:00Z")
		);

		given(connectionService.getActiveConnectionWithFreshToken(userId)).willReturn(Optional.of(connection));
		given(googleCalendarClient.updateEvent(
				eq("access-token"),
				eq("primary"),
				eq("google-event-1"),
				any(GoogleCalendarEventPayload.class)
		)).willReturn(updated);
		given(scheduleCalendarPublicService.upsertGoogleEvent(
				userId,
				"primary",
				null,
				"google-event-1",
				"수정된 일정",
				Instant.parse("2026-07-10T01:00:00Z"),
				Instant.parse("2026-07-10T02:00:00Z"),
				false
		)).willReturn(cached);

		ScheduleResult result = googleCalendarEventService.updateGoogleEvent(
				userId,
				"primary",
				"google-event-1",
				new UpdateScheduleCommand(
						"수정된 일정",
						Instant.parse("2026-07-10T01:00:00Z"),
						Instant.parse("2026-07-10T02:00:00Z"),
						false,
						null,
						null
				)
		);

		assertThat(result).isEqualTo(cached);
		verify(googleCalendarClient).updateEvent(
				eq("access-token"),
				eq("primary"),
				eq("google-event-1"),
				any(GoogleCalendarEventPayload.class)
		);
		verify(scheduleCalendarPublicService).upsertGoogleEvent(
				userId,
				"primary",
				null,
				"google-event-1",
				"수정된 일정",
				Instant.parse("2026-07-10T01:00:00Z"),
				Instant.parse("2026-07-10T02:00:00Z"),
				false
		);
	}

	@Test
	void deleteGoogleEventRemovesGoogleEventAndCachedSchedule() {
		UUID userId = UUID.randomUUID();
		GoogleCalendarConnection connection = GoogleCalendarConnection.create(
				userId,
				"user@example.com",
				"access-token",
				"refresh-token",
				Instant.parse("2026-07-03T00:00:00Z")
		);

		given(connectionService.getActiveConnectionWithFreshToken(userId)).willReturn(Optional.of(connection));

		googleCalendarEventService.deleteGoogleEvent(userId, "primary", "google-event-1");

		verify(googleCalendarClient).deleteEvent("access-token", "primary", "google-event-1");
		verify(scheduleCalendarPublicService).deleteGoogleEventSchedules(userId, List.of("google-event-1"));
		verify(deleteRequestService).markSucceeded(userId, "google-event-1");
	}

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
				"primary",
				"Primary",
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
		given(googleCalendarClient.getEvents("access-token", "primary", from.toString(), to.toString()))
				.willReturn(List.of(cancelled, active));
		given(deleteRequestService.findPendingGoogleEventIds(userId, List.of("google-active"))).willReturn(Set.of());
		given(scheduleCalendarPublicService.upsertGoogleEvent(
				userId,
				"primary",
				"Primary",
				"google-active",
				"검토 회의",
				Instant.parse("2026-07-10T01:00:00Z"),
				Instant.parse("2026-07-10T02:00:00Z"),
				false
		)).willReturn(synced);

		List<ScheduleResult> results = googleCalendarEventService.syncEvents(userId, from, to);

		assertThat(results).containsExactly(synced);
		verify(scheduleCalendarPublicService).deleteGoogleEventSchedules(userId, List.of("google-deleted"));
		verify(deleteRequestService).markSucceeded(userId, List.of("google-deleted"));
		verify(scheduleCalendarPublicService).upsertGoogleEvent(
				userId,
				"primary",
				"Primary",
				"google-active",
				"검토 회의",
				Instant.parse("2026-07-10T01:00:00Z"),
				Instant.parse("2026-07-10T02:00:00Z"),
				false
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
		given(googleCalendarClient.getEvents("access-token", "primary", from.toString(), to.toString()))
				.willReturn(List.of(cancelled));

		List<ScheduleResult> results = googleCalendarEventService.syncEvents(userId, from, to);

		assertThat(results).isEmpty();
		verify(scheduleCalendarPublicService).deleteGoogleEventSchedules(userId, List.of("google-deleted"));
		verify(deleteRequestService).markSucceeded(userId, List.of("google-deleted"));
		verify(scheduleCalendarPublicService, never()).upsertGoogleEvent(any(), any(), any(), any(), any());
	}

	@Test
	void syncEventsRetriesPendingDeleteAndDoesNotUpsertIt() {
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
		GoogleCalendarEventPayload active = new GoogleCalendarEventPayload(
				"google-pending-delete",
				"confirmed",
				"새 작업",
				new GoogleCalendarEventPayload.EventDateTime("2026-07-10T01:00:00Z"),
				new GoogleCalendarEventPayload.EventDateTime("2026-07-10T02:00:00Z")
		);

		given(connectionService.getActiveConnectionWithFreshToken(userId)).willReturn(Optional.of(connection));
		given(googleCalendarClient.getEvents("access-token", "primary", from.toString(), to.toString()))
				.willReturn(List.of(active));
		given(deleteRequestService.findPendingGoogleEventIds(userId, List.of("google-pending-delete")))
				.willReturn(Set.of("google-pending-delete"));

		List<ScheduleResult> results = googleCalendarEventService.syncEvents(userId, from, to);

		assertThat(results).isEmpty();
		verify(googleCalendarClient).deleteEvent("access-token", "primary", "google-pending-delete");
		verify(deleteRequestService).markSucceeded(userId, "google-pending-delete");
		verify(scheduleCalendarPublicService, never()).upsertGoogleEvent(any(), any(), any(), any(), any());
	}
}
