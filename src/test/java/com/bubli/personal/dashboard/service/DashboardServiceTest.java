package com.bubli.personal.dashboard.service;

import com.bubli.agent.service.AgentSuggestionPublicService;
import com.bubli.personal.memo.dto.MemoResult;
import com.bubli.personal.memo.service.MemoPublicService;
import com.bubli.personal.memo.type.MemoStatus;
import com.bubli.personal.notification.service.NotificationPublicService;
import com.bubli.personal.timer.dto.TimeLogResult;
import com.bubli.personal.timer.service.TimeLogPublicService;
import com.bubli.project.dto.ProjectRoomResult;
import com.bubli.project.service.ProjectRoomPublicService;
import com.bubli.project.type.ProjectRoomStatus;
import com.bubli.resource.dto.ResourceAnalysisSummaryResult;
import com.bubli.resource.service.ResourcePublicService;
import com.bubli.work.schedule.dto.ScheduleResult;
import com.bubli.work.schedule.service.SchedulePublicService;
import com.bubli.work.task.dto.TaskResult;
import com.bubli.work.task.service.TaskPublicService;
import com.bubli.work.task.type.TaskStatus;
import com.bubli.work.wbs.dto.WbsItemResult;
import com.bubli.work.wbs.service.WbsItemPublicService;
import com.bubli.work.wbs.type.WbsStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

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

	@Mock
	MemoPublicService memoPublicService;

	@Mock
	ResourcePublicService resourcePublicService;

	@Mock
	ProjectRoomPublicService projectRoomPublicService;

	@Mock
	WbsItemPublicService wbsItemPublicService;

	@InjectMocks
	DashboardService dashboardService;

	@Test
	void getWorkDashboardIncludesMemoResourceAnalysisAndProjectProgressSummaries() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		given(taskPublicService.getDueBetweenTasks(eq(userId), any(), any())).willReturn(List.of());
		given(schedulePublicService.getSchedulesBetween(eq(userId), any(), any())).willReturn(List.of());
		given(notificationPublicService.countUnread(userId)).willReturn(0L);
		given(timeLogPublicService.getRunningTimer(userId)).willReturn(Optional.empty());
		given(agentSuggestionPublicService.getReviewRequiredSummaries(userId, 5)).willReturn(List.of());
		given(memoPublicService.getUpdatedMemosBetween(eq(userId), any(), any(), eq(5))).willReturn(List.of(
				memo(userId, null, "개인 메모"),
				memo(userId, roomId, "프로젝트룸 메모")
		));
		given(resourcePublicService.getRecentAnalysisSummaries(userId, 5)).willReturn(List.of(
				new ResourceAnalysisSummaryResult(
						UUID.randomUUID(),
						"요구사항.pdf",
						"핵심 요구사항 분석 완료",
						Instant.parse("2026-07-01T00:00:00Z")
				)
		));
		given(projectRoomPublicService.getAccessibleRooms(userId, 5)).willReturn(List.of(projectRoom(userId, roomId)));
		given(taskPublicService.getRoomTasksForBoard(roomId)).willReturn(List.of(
				task(roomId, TaskStatus.TODO),
				task(roomId, TaskStatus.IN_PROGRESS),
				task(roomId, TaskStatus.DONE),
				task(roomId, TaskStatus.DONE)
		));
		given(wbsItemPublicService.getRoomItemsForBoard(roomId)).willReturn(List.of(
				wbs(roomId, WbsStatus.TODO),
				wbs(roomId, WbsStatus.IN_PROGRESS),
				wbs(roomId, WbsStatus.DONE)
		));

		var response = dashboardService.getWorkDashboard(userId);

		assertThat(response.runningTimer()).isNull();
		assertThat(response.memoSummary()).containsExactly(
				"개인: 개인 메모",
				"프로젝트룸: 프로젝트룸 메모"
		);
		assertThat(response.recentResourceAnalysisSummary()).containsExactly(
				"자료: 요구사항.pdf - 핵심 요구사항 분석 완료"
		);
		assertThat(response.projectProgressSummary()).hasSize(1);
		assertThat(response.projectProgressSummary().getFirst().roomId()).isEqualTo(roomId);
		assertThat(response.projectProgressSummary().getFirst().roomName()).isEqualTo("Bubli 백엔드");
		assertThat(response.projectProgressSummary().getFirst().totalTasks()).isEqualTo(4);
		assertThat(response.projectProgressSummary().getFirst().todoTasks()).isEqualTo(1);
		assertThat(response.projectProgressSummary().getFirst().inProgressTasks()).isEqualTo(1);
		assertThat(response.projectProgressSummary().getFirst().doneTasks()).isEqualTo(2);
		assertThat(response.projectProgressSummary().getFirst().progressPercent()).isEqualTo(50);
		assertThat(response.projectProgressSummary().getFirst().totalWbsItems()).isEqualTo(3);
		assertThat(response.projectProgressSummary().getFirst().todoWbsItems()).isEqualTo(1);
		assertThat(response.projectProgressSummary().getFirst().inProgressWbsItems()).isEqualTo(1);
		assertThat(response.projectProgressSummary().getFirst().doneWbsItems()).isEqualTo(1);
		assertThat(response.projectProgressSummary().getFirst().wbsProgressPercent()).isEqualTo(33);
	}

	@Test
	void getWorkDashboardCombinesTodayTasksSchedulesNotificationsAndRunningTimer() {
		UUID userId = UUID.randomUUID();
		Instant startOfToday = Instant.now().truncatedTo(ChronoUnit.DAYS);
		Instant startOfTomorrow = startOfToday.plus(1, ChronoUnit.DAYS);
		Instant endOfWeek = startOfToday.plus(7, ChronoUnit.DAYS);
		TaskResult todayTask = task(null, TaskStatus.TODO);
		TaskResult upcomingTask = task(null, TaskStatus.TODO);
		ScheduleResult todaySchedule = schedule(userId);
		TimeLogResult runningTimer = runningTimer(userId);
		given(taskPublicService.getDueBetweenTasks(userId, startOfToday, startOfTomorrow)).willReturn(List.of(todayTask));
		given(taskPublicService.getDueBetweenTasks(userId, startOfTomorrow, endOfWeek)).willReturn(List.of(upcomingTask));
		given(schedulePublicService.getSchedulesBetween(userId, startOfToday, startOfTomorrow)).willReturn(List.of(todaySchedule));
		given(notificationPublicService.countUnread(userId)).willReturn(3L);
		given(timeLogPublicService.getRunningTimer(userId)).willReturn(Optional.of(runningTimer));
		given(agentSuggestionPublicService.getReviewRequiredSummaries(userId, 5)).willReturn(List.of());
		given(memoPublicService.getUpdatedMemosBetween(eq(userId), any(), any(), eq(5))).willReturn(List.of());
		given(resourcePublicService.getRecentAnalysisSummaries(userId, 5)).willReturn(List.of());
		given(projectRoomPublicService.getAccessibleRooms(userId, 5)).willReturn(List.of());

		var response = dashboardService.getWorkDashboard(userId);

		assertThat(response.todayTasks()).containsExactly(todayTask);
		assertThat(response.upcomingDeadlines()).containsExactly(upcomingTask);
		assertThat(response.todaySchedules()).containsExactly(todaySchedule);
		assertThat(response.unreadNotificationCount()).isEqualTo(3L);
		assertThat(response.runningTimer()).isEqualTo(runningTimer);
	}

	private TimeLogResult runningTimer(UUID userId) {
		Instant now = Instant.parse("2026-07-01T00:00:00Z");
		return new TimeLogResult(
				UUID.randomUUID(),
				userId,
				null,
				com.bubli.personal.timer.type.TimerType.GENERAL,
				"dashboard-timer-key",
				null,
				com.bubli.personal.timer.type.TimeLogStatus.RUNNING,
				now,
				now,
				null,
				0L,
				now,
				now,
				now
		);
	}

	private ScheduleResult schedule(UUID userId) {
		Instant now = Instant.parse("2026-07-01T00:00:00Z");
		return new ScheduleResult(
				UUID.randomUUID(),
				userId,
				null,
				null,
				null,
				null,
				null,
				null,
				"오늘 일정",
				now,
				now.plusSeconds(3600),
				false,
				com.bubli.work.schedule.type.ScheduleSyncStatus.LOCAL_ONLY,
				null,
				now,
				now
		);
	}

	private MemoResult memo(UUID userId, UUID roomId, String body) {
		Instant now = Instant.parse("2026-07-01T00:00:00Z");
		return new MemoResult(
				UUID.randomUUID(),
				userId,
				roomId,
				body,
				MemoStatus.ACTIVE,
				now,
				now
		);
	}

	private ProjectRoomResult projectRoom(UUID userId, UUID roomId) {
		Instant now = Instant.parse("2026-07-01T00:00:00Z");
		return new ProjectRoomResult(
				roomId,
				userId,
				"Bubli 백엔드",
				null,
				null,
				null,
				null,
				null,
				ProjectRoomStatus.ACTIVE,
				null,
				now,
				now
		);
	}

	private TaskResult task(UUID roomId, TaskStatus status) {
		Instant now = Instant.parse("2026-07-01T00:00:00Z");
		return new TaskResult(
				UUID.randomUUID(),
				null,
				null,
				roomId,
				null,
				status.name(),
				null,
				status,
				null,
				now,
				now
		);
	}

	private WbsItemResult wbs(UUID roomId, WbsStatus status) {
		Instant now = Instant.parse("2026-07-01T00:00:00Z");
		return new WbsItemResult(
				UUID.randomUUID(),
				roomId,
				null,
				status.name(),
				1,
				status,
				now,
				now
		);
	}
}
