package com.bubli.widget.service;

import com.bubli.agent.service.AgentSuggestionPublicService;
import com.bubli.personal.notification.service.NotificationPublicService;
import com.bubli.personal.timer.dto.TimeLogResult;
import com.bubli.personal.timer.service.TimeLogPublicService;
import com.bubli.personal.timer.type.TimeLogStatus;
import com.bubli.personal.timer.type.TimerType;
import com.bubli.project.service.ProjectMembershipPublicService;
import com.bubli.widget.dto.WidgetSummaryResponse;
import com.bubli.widget.entity.WidgetBubbleSetting;
import com.bubli.widget.entity.WidgetContextSetting;
import com.bubli.widget.repository.WidgetBubbleSettingRepository;
import com.bubli.widget.repository.WidgetContextSettingRepository;
import com.bubli.widget.repository.WidgetDailySummaryRepository;
import com.bubli.widget.repository.WidgetItemStateRepository;
import com.bubli.widget.type.BubbleType;
import com.bubli.work.schedule.dto.ScheduleResult;
import com.bubli.work.schedule.service.SchedulePublicService;
import com.bubli.work.schedule.type.ScheduleSyncStatus;
import com.bubli.work.task.dto.TaskResult;
import com.bubli.work.task.service.TaskPublicService;
import com.bubli.work.task.type.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WidgetServiceTest {

	@Mock
	WidgetBubbleSettingRepository bubbleSettingRepository;

	@Mock
	WidgetContextSettingRepository contextSettingRepository;

	@Mock
	WidgetItemStateRepository itemStateRepository;

	@Mock
	WidgetDailySummaryRepository dailySummaryRepository;

	@Mock
	ProjectMembershipPublicService projectMembershipPublicService;

	@Mock
	TaskPublicService taskPublicService;

	@Mock
	SchedulePublicService schedulePublicService;

	@Mock
	NotificationPublicService notificationPublicService;

	@Mock
	TimeLogPublicService timeLogPublicService;

	@Mock
	AgentSuggestionPublicService agentSuggestionPublicService;

	WidgetService widgetService;

	@BeforeEach
	void setUp() {
		widgetService = new WidgetService(
				bubbleSettingRepository,
				contextSettingRepository,
				itemStateRepository,
				dailySummaryRepository,
				projectMembershipPublicService,
				taskPublicService,
				schedulePublicService,
				notificationPublicService,
				timeLogPublicService,
				agentSuggestionPublicService
		);
	}

	@Test
	void updateContextValidatesSelectedRoomMembership() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		WidgetContextSetting context = WidgetContextSetting.create(userId, null);
		given(contextSettingRepository.findByUserId(userId)).willReturn(Optional.of(context));

		widgetService.updateContext(userId, roomId);

		verify(projectMembershipPublicService).assertActiveMember(userId, roomId);
		assertThat(context.getSelectedRoomId()).isEqualTo(roomId);
		assertThat(context.getMode().name()).isEqualTo("ROOM");
	}

	@Test
	void getSummaryReturnsPersonalWidgetData() {
		UUID userId = UUID.randomUUID();
		TaskResult task = task(userId, null, TaskStatus.TODO, Instant.parse("2026-07-01T03:00:00Z"));
		ScheduleResult schedule = schedule(userId, null);
		TimeLogResult timer = timer(userId, null);
		given(contextSettingRepository.findByUserId(userId)).willReturn(Optional.empty());
		given(bubbleSettingRepository.findByUserId(userId)).willReturn(List.of(
				WidgetBubbleSetting.create(userId, BubbleType.TODO)
		));
		given(taskPublicService.getDueBetweenTasks(eq(userId), any(Instant.class), any(Instant.class)))
				.willReturn(List.of(task));
		given(schedulePublicService.getSchedulesBetween(eq(userId), any(Instant.class), any(Instant.class)))
				.willReturn(List.of(schedule));
		given(notificationPublicService.countUnread(userId)).willReturn(3L);
		given(timeLogPublicService.getRunningTimer(userId)).willReturn(Optional.of(timer));
		given(agentSuggestionPublicService.getReviewRequiredSummaries(userId, 5)).willReturn(List.of("검토 필요"));

		WidgetSummaryResponse response = widgetService.getSummary(userId);

		assertThat(response.context().mode()).isEqualTo("PERSONAL");
		assertThat(response.bubbles()).hasSize(1);
		assertThat(response.tasks()).containsExactly(task);
		assertThat(response.schedules()).containsExactly(schedule);
		assertThat(response.unreadNotificationCount()).isEqualTo(3);
		assertThat(response.runningTimer()).isEqualTo(timer);
		assertThat(response.agentSuggestionSummary()).containsExactly("검토 필요");
	}

	@Test
	void getSummaryReturnsRoomWidgetDataAndFiltersCompletedTasks() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		WidgetContextSetting context = WidgetContextSetting.create(userId, roomId);
		TaskResult todoTask = task(userId, roomId, TaskStatus.TODO, Instant.parse("2026-07-01T03:00:00Z"));
		TaskResult doneTask = task(userId, roomId, TaskStatus.DONE, Instant.parse("2026-07-01T02:00:00Z"));
		ScheduleResult schedule = schedule(userId, roomId);
		given(contextSettingRepository.findByUserId(userId)).willReturn(Optional.of(context));
		given(bubbleSettingRepository.findByUserId(userId)).willReturn(List.of());
		given(taskPublicService.getRoomTasksForBoard(roomId)).willReturn(List.of(doneTask, todoTask));
		given(schedulePublicService.getRoomSchedulesBetween(eq(roomId), any(Instant.class), any(Instant.class)))
				.willReturn(List.of(schedule));
		given(notificationPublicService.countUnread(userId)).willReturn(1L);
		given(timeLogPublicService.getRunningTimer(userId)).willReturn(Optional.empty());
		given(agentSuggestionPublicService.getReviewRequiredSummaries(userId, 5)).willReturn(List.of());

		WidgetSummaryResponse response = widgetService.getSummary(userId);

		verify(projectMembershipPublicService).assertActiveMember(userId, roomId);
		assertThat(response.context().selectedRoomId()).isEqualTo(roomId);
		assertThat(response.context().mode()).isEqualTo("ROOM");
		assertThat(response.tasks()).containsExactly(todoTask);
		assertThat(response.schedules()).containsExactly(schedule);
		assertThat(response.unreadNotificationCount()).isEqualTo(1);
		assertThat(response.runningTimer()).isNull();
	}

	private TaskResult task(UUID userId, UUID roomId, TaskStatus status, Instant dueAt) {
		Instant now = Instant.parse("2026-07-01T00:00:00Z");
		return new TaskResult(
				UUID.randomUUID(),
				roomId == null ? userId : null,
				roomId == null ? null : userId,
				roomId,
				null,
				"작업",
				"설명",
				status,
				dueAt,
				now,
				now
		);
	}

	private ScheduleResult schedule(UUID userId, UUID roomId) {
		Instant now = Instant.parse("2026-07-01T00:00:00Z");
		return new ScheduleResult(
				UUID.randomUUID(),
				userId,
				roomId,
				null,
				null,
				null,
				"일정",
				now.plusSeconds(3600),
				now.plusSeconds(7200),
				false,
				ScheduleSyncStatus.LOCAL_ONLY,
				null,
				now,
				now
		);
	}

	private TimeLogResult timer(UUID userId, UUID roomId) {
		Instant now = Instant.parse("2026-07-01T00:00:00Z");
		return new TimeLogResult(
				UUID.randomUUID(),
				userId,
				roomId,
				TimerType.GENERAL,
				"timer-key",
				null,
				TimeLogStatus.RUNNING,
				now,
				now,
				null,
				0L,
				now,
				now,
				now
		);
	}
}
