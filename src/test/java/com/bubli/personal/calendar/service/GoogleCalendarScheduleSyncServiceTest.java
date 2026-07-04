package com.bubli.personal.calendar.service;

import com.bubli.personal.calendar.dto.GoogleCalendarEventPayload;
import com.bubli.personal.calendar.dto.GoogleCalendarSyncResult;
import com.bubli.personal.calendar.entity.GoogleCalendarConnection;
import com.bubli.personal.calendar.entity.ProjectRoomGoogleCalendar;
import com.bubli.work.schedule.dto.ScheduleSyncTarget;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GoogleCalendarScheduleSyncServiceTest {

	@Mock
	GoogleCalendarConnectionService connectionService;

	@Mock
	GoogleCalendarClient googleCalendarClient;

	@Mock
	GoogleCalendarDeleteRequestService deleteRequestService;

	@Mock
	ProjectRoomCalendarService projectRoomCalendarService;

	@InjectMocks
	GoogleCalendarScheduleSyncService syncService;

	@Test
	void roomScheduleCreatesGoogleEventInProjectRoomCalendar() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		GoogleCalendarConnection connection = GoogleCalendarConnection.create(
				userId,
				"user@example.com",
				"access-token",
				"refresh-token",
				Instant.parse("2026-07-05T01:00:00Z")
		);
		ProjectRoomGoogleCalendar roomCalendar = ProjectRoomGoogleCalendar.create(
				userId,
				roomId,
				"room-calendar-id",
				"A 프로젝트룸"
		);
		GoogleCalendarEventPayload created = new GoogleCalendarEventPayload(
				"google-event-1",
				"confirmed",
				"프로젝트룸 회의",
				new GoogleCalendarEventPayload.EventDateTime("2026-07-05T01:00:00Z"),
				new GoogleCalendarEventPayload.EventDateTime("2026-07-05T02:00:00Z")
		);

		given(connectionService.getActiveConnectionWithFreshToken(userId)).willReturn(Optional.of(connection));
		given(projectRoomCalendarService.ensureRoomCalendar(userId, roomId)).willReturn(Optional.of(roomCalendar));
		given(googleCalendarClient.createEvent(eq("access-token"), eq("room-calendar-id"), any(GoogleCalendarEventPayload.class)))
				.willReturn(created);

		GoogleCalendarSyncResult result = syncService.syncCreatedOrUpdatedSchedule(
				userId,
				new ScheduleSyncTarget(
						roomId,
						null,
						null,
						null,
						"프로젝트룸 회의",
						Instant.parse("2026-07-05T01:00:00Z"),
						Instant.parse("2026-07-05T02:00:00Z")
				)
		);

		assertThat(result.succeeded()).isTrue();
		assertThat(result.googleCalendarId()).isEqualTo("room-calendar-id");
		assertThat(result.googleCalendarSummary()).isEqualTo("A 프로젝트룸");
		verify(googleCalendarClient).createEvent(
				eq("access-token"),
				eq("room-calendar-id"),
				any(GoogleCalendarEventPayload.class)
		);
	}
}
