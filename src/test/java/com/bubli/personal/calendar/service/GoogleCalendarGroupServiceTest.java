package com.bubli.personal.calendar.service;

import com.bubli.global.response.PageResponse;
import com.bubli.personal.calendar.dto.CalendarEventGroupResponse;
import com.bubli.personal.calendar.dto.GoogleCalendarEventPayload;
import com.bubli.personal.calendar.dto.GoogleCalendarListEntry;
import com.bubli.personal.calendar.entity.GoogleCalendarConnection;
import com.bubli.personal.calendar.type.CalendarEventGroupType;
import com.bubli.project.dto.ProjectRoomResult;
import com.bubli.project.service.ProjectRoomPublicService;
import com.bubli.work.schedule.dto.ScheduleResult;
import com.bubli.work.schedule.service.ScheduleCalendarPublicService;
import com.bubli.work.schedule.type.ScheduleSyncStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GoogleCalendarGroupServiceTest {

	@Mock
	ScheduleCalendarPublicService scheduleCalendarPublicService;

	@Mock
	ProjectRoomPublicService projectRoomPublicService;

	@Mock
	GoogleCalendarConnectionService connectionService;

	@Mock
	GoogleCalendarClient googleCalendarClient;

	@Mock
	ProjectRoomCalendarService projectRoomCalendarService;

	@InjectMocks
	GoogleCalendarGroupService groupService;

	@Test
	void getGroupedEventsDoesNotDuplicateProjectRoomGoogleCalendarAsGoogleGroup() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		Instant from = Instant.parse("2026-07-05T00:00:00Z");
		Instant to = Instant.parse("2026-07-06T00:00:00Z");
		GoogleCalendarConnection connection = GoogleCalendarConnection.create(
				userId,
				"user@example.com",
				"access-token",
				"refresh-token",
				Instant.parse("2026-07-05T03:00:00Z")
		);
		ScheduleResult roomSchedule = new ScheduleResult(
				UUID.randomUUID(),
				userId,
				roomId,
				null,
				null,
				"google-room-event",
				"room-calendar-id",
				"A 프로젝트룸",
				"룸 회의",
				Instant.parse("2026-07-05T01:00:00Z"),
				Instant.parse("2026-07-05T02:00:00Z"),
				false,
				ScheduleSyncStatus.SYNCED,
				Instant.parse("2026-07-05T01:10:00Z"),
				Instant.parse("2026-07-05T00:50:00Z"),
				Instant.parse("2026-07-05T00:50:00Z")
		);
		GoogleCalendarEventPayload personalGoogleEvent = new GoogleCalendarEventPayload(
				"primary-event",
				"confirmed",
				"개인 구글 일정",
				new GoogleCalendarEventPayload.EventDateTime("2026-07-05T04:00:00Z"),
				new GoogleCalendarEventPayload.EventDateTime("2026-07-05T05:00:00Z")
		);

		given(connectionService.hasActiveConnection(userId)).willReturn(true);
		given(scheduleCalendarPublicService.getEvents(userId, null, from, to, PageRequest.of(0, 200)))
				.willReturn(new PageResponse<>(List.of(roomSchedule), 0, 200, 1, 1, false));
		given(projectRoomPublicService.getProjectRoom(userId, roomId))
				.willReturn(new ProjectRoomResult(
						roomId,
						userId,
						"A 프로젝트룸",
						null,
						null,
						null,
						null,
						null,
						null,
						null,
						null,
						null
				));
		given(projectRoomCalendarService.findGoogleCalendarIds(eq(userId), anyCollection()))
				.willReturn(Map.of(roomId, "room-calendar-id"));
		given(projectRoomCalendarService.findManagedGoogleCalendarIds(userId)).willReturn(Set.of("room-calendar-id"));
		given(connectionService.getActiveConnectionWithFreshToken(userId)).willReturn(Optional.of(connection));
		given(googleCalendarClient.getCalendars("access-token")).willReturn(List.of(
				new GoogleCalendarListEntry("room-calendar-id", "A 프로젝트룸", false, "owner", true, null),
				new GoogleCalendarListEntry("primary", "개인", true, "owner", true, null)
		));
		given(googleCalendarClient.getEvents("access-token", "primary", from.toString(), to.toString()))
				.willReturn(List.of(personalGoogleEvent));

		List<CalendarEventGroupResponse> result = groupService.getGroupedEvents(
				userId,
				null,
				from,
				to,
				null,
				null
		);

		assertThat(result)
				.extracting(CalendarEventGroupResponse::groupType)
				.contains(CalendarEventGroupType.PROJECT_ROOM, CalendarEventGroupType.GOOGLE_CALENDAR);
		assertThat(result)
				.noneMatch(group -> group.groupType() == CalendarEventGroupType.GOOGLE_CALENDAR
						&& "room-calendar-id".equals(group.googleCalendarId()));
		verify(googleCalendarClient, never())
				.getEvents("access-token", "room-calendar-id", from.toString(), to.toString());
	}
}
