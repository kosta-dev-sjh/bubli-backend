package com.bubli.work.schedule.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.personal.calendar.dto.GoogleCalendarSyncResult;
import com.bubli.personal.calendar.service.GoogleCalendarScheduleSyncPublicService;
import com.bubli.project.service.ProjectMembershipPublicService;
import com.bubli.work.schedule.dto.CreateScheduleCommand;
import com.bubli.work.schedule.dto.ScheduleResult;
import com.bubli.work.schedule.dto.ScheduleSyncTarget;
import com.bubli.work.schedule.entity.Schedule;
import com.bubli.work.schedule.repository.ScheduleRepository;
import com.bubli.work.schedule.type.ScheduleSyncStatus;
import com.bubli.work.task.service.TaskPublicService;
import com.bubli.work.wbs.service.WbsItemPublicService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SchedulePublicServiceImplTest {

	@Mock
	ScheduleRepository scheduleRepository;

	@Mock
	ProjectMembershipPublicService projectMembershipPublicService;

	@Mock
	TaskPublicService taskPublicService;

	@Mock
	WbsItemPublicService wbsItemPublicService;

	@Mock
	GoogleCalendarScheduleSyncPublicService googleCalendarScheduleSyncPublicService;

	@InjectMocks
	SchedulePublicServiceImpl schedulePublicService;

	@Test
	void createRoomScheduleValidatesLinkedTaskAndWbsScope() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID taskId = UUID.randomUUID();
		UUID wbsItemId = UUID.randomUUID();
		given(scheduleRepository.save(any(Schedule.class))).willAnswer(invocation -> {
			Schedule schedule = invocation.getArgument(0);
			ReflectionTestUtils.setField(schedule, "id", UUID.randomUUID());
			return schedule;
		});

		schedulePublicService.create(userId, new CreateScheduleCommand(
				roomId,
				taskId,
				wbsItemId,
				"에이전트 일정 후보",
				Instant.parse("2026-07-10T01:00:00Z"),
				Instant.parse("2026-07-10T02:00:00Z"),
				false
		));

		verify(projectMembershipPublicService).assertActiveMember(userId, roomId);
		verify(wbsItemPublicService).assertRoomWbsItem(roomId, wbsItemId);
		verify(taskPublicService).assertScheduleTaskScope(userId, roomId, taskId);
	}

	@Test
	void createScheduleRejectsWbsItemWithoutRoomId() {
		UUID userId = UUID.randomUUID();
		UUID wbsItemId = UUID.randomUUID();

		assertThatThrownBy(() -> schedulePublicService.create(userId, new CreateScheduleCommand(
				null,
				null,
				wbsItemId,
				"잘못된 WBS 일정",
				Instant.parse("2026-07-10T01:00:00Z"),
				Instant.parse("2026-07-10T02:00:00Z"),
				false
		))).isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SCHEDULE_400_001));
	}

	@Test
	void createScheduleKeepsLocalScheduleWhenGoogleSyncThrows() {
		UUID userId = UUID.randomUUID();
		given(scheduleRepository.save(any(Schedule.class))).willAnswer(invocation -> {
			Schedule schedule = invocation.getArgument(0);
			ReflectionTestUtils.setField(schedule, "id", UUID.randomUUID());
			return schedule;
		});
		willThrow(new IllegalStateException("calendar unavailable"))
				.given(googleCalendarScheduleSyncPublicService)
				.syncCreatedOrUpdatedSchedule(eq(userId), any(ScheduleSyncTarget.class));

		ScheduleResult result = schedulePublicService.create(userId, new CreateScheduleCommand(
				null,
				null,
				null,
				"개인 일정",
				Instant.parse("2026-07-10T01:00:00Z"),
				Instant.parse("2026-07-10T02:00:00Z"),
				false
		));

		assertThat(result.syncStatus()).isEqualTo(ScheduleSyncStatus.SYNC_FAILED);

		ArgumentCaptor<Schedule> scheduleCaptor = ArgumentCaptor.forClass(Schedule.class);
		verify(scheduleRepository).save(scheduleCaptor.capture());
		assertThat(scheduleCaptor.getValue().getTitle()).isEqualTo("개인 일정");
		assertThat(scheduleCaptor.getValue().getSyncStatus()).isEqualTo(ScheduleSyncStatus.SYNC_FAILED);
	}

	@Test
	void createScheduleMarksSyncedWhenGoogleSyncSucceeds() {
		UUID userId = UUID.randomUUID();
		given(scheduleRepository.save(any(Schedule.class))).willAnswer(invocation -> {
			Schedule schedule = invocation.getArgument(0);
			ReflectionTestUtils.setField(schedule, "id", UUID.randomUUID());
			return schedule;
		});
		given(googleCalendarScheduleSyncPublicService.syncCreatedOrUpdatedSchedule(
				eq(userId),
				any(ScheduleSyncTarget.class)
		)).willReturn(GoogleCalendarSyncResult.succeeded("google-event-1", "primary", "기본"));

		ScheduleResult result = schedulePublicService.create(userId, new CreateScheduleCommand(
				null,
				null,
				null,
				"개인 일정",
				Instant.parse("2026-07-10T01:00:00Z"),
				Instant.parse("2026-07-10T02:00:00Z"),
				false
		));

		assertThat(result.syncStatus()).isEqualTo(ScheduleSyncStatus.SYNCED);
		assertThat(result.googleEventId()).isEqualTo("google-event-1");
		assertThat(result.googleCalendarId()).isEqualTo("primary");
	}
}
