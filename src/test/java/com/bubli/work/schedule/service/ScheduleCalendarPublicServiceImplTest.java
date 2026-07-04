package com.bubli.work.schedule.service;

import com.bubli.personal.calendar.service.GoogleCalendarScheduleSyncPublicService;
import com.bubli.work.schedule.entity.Schedule;
import com.bubli.work.schedule.repository.ScheduleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ScheduleCalendarPublicServiceImplTest {

	@Mock
	ScheduleService scheduleService;

	@Mock
	ScheduleRepository scheduleRepository;

	@Mock
	GoogleCalendarScheduleSyncPublicService googleCalendarScheduleSyncPublicService;

	@InjectMocks
	ScheduleCalendarPublicServiceImpl service;

	@Test
	void deleteGoogleEventSchedulesDeletesOnlyMatchingCalendarEvents() {
		UUID userId = UUID.randomUUID();
		Schedule roomSchedule = Schedule.create(
				userId,
				null,
				null,
				null,
				"룸 일정",
				Instant.parse("2026-07-05T01:00:00Z"),
				Instant.parse("2026-07-05T02:00:00Z"),
				false
		);
		roomSchedule.markSynced("room-calendar-id", "프로젝트룸", "same-event-id");
		given(scheduleRepository.findByOwnerUserIdAndGoogleCalendarIdAndGoogleEventIdIn(
				userId,
				"room-calendar-id",
				List.of("same-event-id")
		)).willReturn(List.of(roomSchedule));

		service.deleteGoogleEventSchedules(userId, "room-calendar-id", List.of("same-event-id"));

		verify(scheduleRepository).deleteAll(List.of(roomSchedule));
		verify(scheduleRepository, never()).findByOwnerUserIdAndGoogleCalendarIdAndGoogleEventIdIn(
				userId,
				"primary",
				List.of("same-event-id")
		);
	}

	@Test
	void deleteGoogleEventSchedulesKeepsLegacyPrimaryDefault() {
		UUID userId = UUID.randomUUID();

		service.deleteGoogleEventSchedules(userId, List.of("google-event-1"));

		verify(scheduleRepository).findByOwnerUserIdAndGoogleCalendarIdAndGoogleEventIdIn(
				userId,
				"primary",
				List.of("google-event-1")
		);
	}
}
