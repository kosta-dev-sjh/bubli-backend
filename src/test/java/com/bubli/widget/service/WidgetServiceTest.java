package com.bubli.widget.service;

import com.bubli.agent.service.AgentSuggestionPublicService;
import com.bubli.personal.notification.service.NotificationPublicService;
import com.bubli.personal.timer.dto.TimeLogResult;
import com.bubli.personal.timer.service.TimeLogPublicService;
import com.bubli.personal.timer.type.TimeLogStatus;
import com.bubli.personal.timer.type.TimerType;
import com.bubli.project.service.ProjectMembershipPublicService;
import com.bubli.widget.dto.WidgetSummaryResponse;
import com.bubli.widget.dto.BubbleSettingUpdate;
import com.bubli.widget.dto.WidgetSettingsResponse;
import com.bubli.widget.entity.WidgetBubbleSetting;
import com.bubli.widget.entity.WidgetContextSetting;
import com.bubli.widget.entity.WidgetItemState;
import com.bubli.widget.repository.WidgetBubbleSettingRepository;
import com.bubli.widget.repository.WidgetContextSettingRepository;
import com.bubli.widget.repository.WidgetDailySummaryRepository;
import com.bubli.widget.repository.WidgetItemStateRepository;
import com.bubli.widget.type.BubbleType;
import com.bubli.widget.type.WidgetItemStateValue;
import com.bubli.widget.type.WidgetItemType;
import com.bubli.work.schedule.dto.ScheduleResult;
import com.bubli.work.schedule.service.SchedulePublicService;
import com.bubli.work.schedule.type.ScheduleSyncStatus;
import com.bubli.work.task.dto.TaskResult;
import com.bubli.work.task.service.TaskPublicService;
import com.bubli.work.task.type.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
	void getSettingsCreatesMissingDefaultBubbles() {
		UUID userId = UUID.randomUUID();
		given(bubbleSettingRepository.findByUserId(userId)).willReturn(List.of(
				WidgetBubbleSetting.create(userId, BubbleType.TODO)
		), allBubbleSettings(userId));

		WidgetSettingsResponse response = widgetService.getSettings(userId);

		assertThat(response.bubbles()).extracting("bubbleType").containsExactly(
				"TODO",
				"AGENT",
				"CHAT",
				"TIMER",
				"MEMO",
				"SCHEDULE",
				"RESOURCE",
				"ALERT"
		);
		verify(bubbleSettingRepository, times(BubbleType.values().length - 1))
				.insertDefaultIfAbsent(any(UUID.class), eq(userId), any(String.class));
	}

	@Test
	void getSettingsRereadsDefaultsWhenConcurrentInsertWins() {
		UUID userId = UUID.randomUUID();
		given(bubbleSettingRepository.findByUserId(userId)).willReturn(List.of(), allBubbleSettings(userId));
		given(bubbleSettingRepository.insertDefaultIfAbsent(any(UUID.class), eq(userId), any(String.class)))
				.willReturn(0);

		WidgetSettingsResponse response = widgetService.getSettings(userId);

		assertThat(response.bubbles()).extracting("bubbleType").containsExactly(
				"TODO",
				"AGENT",
				"CHAT",
				"TIMER",
				"MEMO",
				"SCHEDULE",
				"RESOURCE",
				"ALERT"
		);
		verify(bubbleSettingRepository, times(BubbleType.values().length))
				.insertDefaultIfAbsent(any(UUID.class), eq(userId), any(String.class));
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
		), allBubbleSettings(userId));
		given(taskPublicService.getDueBetweenTasks(eq(userId), any(Instant.class), any(Instant.class)))
				.willReturn(List.of(task));
		given(schedulePublicService.getSchedulesBetween(eq(userId), any(Instant.class), any(Instant.class)))
				.willReturn(List.of(schedule));
		given(notificationPublicService.countUnread(userId)).willReturn(3L);
		given(timeLogPublicService.getRunningTimer(userId)).willReturn(Optional.of(timer));
		given(agentSuggestionPublicService.getReviewRequiredSummaries(userId, 5)).willReturn(List.of("검토 필요"));

		WidgetSummaryResponse response = widgetService.getSummary(userId);

		assertThat(response.context().mode()).isEqualTo("PERSONAL");
		assertThat(response.bubbles()).hasSize(BubbleType.values().length);
		assertThat(response.tasks()).containsExactly(task);
		assertThat(response.schedules()).containsExactly(schedule);
		assertThat(response.unreadNotificationCount()).isEqualTo(3);
		assertThat(response.runningTimer()).isEqualTo(timer);
		assertThat(response.agentSuggestionSummary()).containsExactly("검토 필요");
	}

	@Test
	void updateSettingsAcceptsResourceAndAlertBubbles() {
		UUID userId = UUID.randomUUID();
		WidgetBubbleSetting resource = WidgetBubbleSetting.create(userId, BubbleType.RESOURCE);
		WidgetBubbleSetting alert = WidgetBubbleSetting.create(userId, BubbleType.ALERT);
		given(bubbleSettingRepository.findByUserIdAndBubbleType(userId, BubbleType.RESOURCE))
				.willReturn(Optional.empty(), Optional.of(resource));
		given(bubbleSettingRepository.findByUserIdAndBubbleType(userId, BubbleType.ALERT))
				.willReturn(Optional.empty(), Optional.of(alert));
		given(bubbleSettingRepository.findByUserId(userId)).willReturn(List.of(
				resource,
				alert
		), allBubbleSettings(userId));

		var response = widgetService.updateSettings(userId, List.of(
				new BubbleSettingUpdate("RESOURCE", true, 120, 160, 320, 420, false, BigDecimal.ONE, false, true),
				new BubbleSettingUpdate("ALERT", true, 180, 220, 300, 360, true, BigDecimal.valueOf(0.80), false, true)
		));

		assertThat(response.bubbles()).extracting("bubbleType").containsExactly(
				"TODO",
				"AGENT",
				"CHAT",
				"TIMER",
				"MEMO",
				"SCHEDULE",
				"RESOURCE",
				"ALERT"
		);
		verify(bubbleSettingRepository, times(BubbleType.values().length))
				.insertDefaultIfAbsent(any(UUID.class), eq(userId), any(String.class));
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
		given(bubbleSettingRepository.findByUserId(userId)).willReturn(List.of(), allBubbleSettings(userId));
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

	@Test
	void getSummaryUsesRequestedRoomWithoutStoredContextUpdate() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		TaskResult task = task(userId, roomId, TaskStatus.TODO, Instant.parse("2026-07-01T03:00:00Z"));
		ScheduleResult schedule = schedule(userId, roomId);
		given(bubbleSettingRepository.findByUserId(userId)).willReturn(List.of(), allBubbleSettings(userId));
		given(taskPublicService.getRoomTasksForBoard(roomId)).willReturn(List.of(task));
		given(schedulePublicService.getRoomSchedulesBetween(eq(roomId), any(Instant.class), any(Instant.class)))
				.willReturn(List.of(schedule));
		given(notificationPublicService.countUnread(userId)).willReturn(0L);
		given(timeLogPublicService.getRunningTimer(userId)).willReturn(Optional.empty());
		given(agentSuggestionPublicService.getReviewRequiredSummaries(userId, 5)).willReturn(List.of());

		WidgetSummaryResponse response = widgetService.getSummary(userId, roomId);

		verify(contextSettingRepository, never()).findByUserId(userId);
		verify(projectMembershipPublicService).assertActiveMember(userId, roomId);
		assertThat(response.context().selectedRoomId()).isEqualTo(roomId);
		assertThat(response.context().mode()).isEqualTo("ROOM");
		assertThat(response.tasks()).containsExactly(task);
		assertThat(response.schedules()).containsExactly(schedule);
	}

	@Test
	void getItemStatesReturnsOnlyRequestedUserStates() {
		UUID userId = UUID.randomUUID();
		UUID taskId = UUID.randomUUID();
		WidgetItemState itemState = WidgetItemState.create(userId, BubbleType.TODO, WidgetItemType.TASK, taskId);
		itemState.updateState(WidgetItemStateValue.PINNED);
		given(itemStateRepository.findByUserIdAndItemIdIn(eq(userId), any())).willReturn(List.of(itemState));

		var response = widgetService.getItemStates(userId, List.of(taskId, taskId));

		assertThat(response).hasSize(1);
		assertThat(response.getFirst().itemId()).isEqualTo(taskId);
		assertThat(response.getFirst().state()).isEqualTo("PINNED");
		verify(itemStateRepository).findByUserIdAndItemIdIn(eq(userId), eq(List.of(taskId)));
	}

	@Test
	void updateItemStateUpdatesExistingStateById() {
		UUID userId = UUID.randomUUID();
		UUID itemStateId = UUID.randomUUID();
		UUID taskId = UUID.randomUUID();
		WidgetItemState itemState = WidgetItemState.create(userId, BubbleType.TODO, WidgetItemType.TASK, taskId);
		given(itemStateRepository.findById(itemStateId)).willReturn(Optional.of(itemState));

		widgetService.updateItemState(userId, itemStateId, null, null, null, "PINNED");

		assertThat(itemState.getState()).isEqualTo(WidgetItemStateValue.PINNED);
		verify(itemStateRepository, never()).save(any());
	}

	@Test
	void updateItemStateCreatesStateFromItemTupleWhenStateIdIsNotKnown() {
		UUID userId = UUID.randomUUID();
		UUID taskId = UUID.randomUUID();
		given(itemStateRepository.findById(taskId)).willReturn(Optional.empty());
		given(itemStateRepository.findByUserIdAndBubbleTypeAndItemTypeAndItemId(
				userId, BubbleType.TODO, WidgetItemType.TASK, taskId
		)).willReturn(Optional.empty());
		given(itemStateRepository.save(any(WidgetItemState.class))).willAnswer(invocation -> invocation.getArgument(0));

		widgetService.updateItemState(userId, taskId, "TODO", taskId, "TASK", "CONFIRMED");

		ArgumentCaptor<WidgetItemState> savedState = ArgumentCaptor.forClass(WidgetItemState.class);
		verify(itemStateRepository).save(savedState.capture());
		assertThat(savedState.getValue().getUserId()).isEqualTo(userId);
		assertThat(savedState.getValue().getBubbleType()).isEqualTo(BubbleType.TODO);
		assertThat(savedState.getValue().getItemType()).isEqualTo(WidgetItemType.TASK);
		assertThat(savedState.getValue().getItemId()).isEqualTo(taskId);
		assertThat(savedState.getValue().getState()).isEqualTo(WidgetItemStateValue.CONFIRMED);
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

	private List<WidgetBubbleSetting> allBubbleSettings(UUID userId) {
		return Arrays.stream(BubbleType.values())
				.map(type -> WidgetBubbleSetting.create(userId, type))
				.toList();
	}
}
