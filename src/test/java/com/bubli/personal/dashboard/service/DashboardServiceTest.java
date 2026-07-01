package com.bubli.personal.dashboard.service;

import com.bubli.agent.service.AgentSuggestionPublicService;
import com.bubli.personal.memo.dto.MemoResult;
import com.bubli.personal.memo.service.MemoPublicService;
import com.bubli.personal.memo.type.MemoStatus;
import com.bubli.personal.notification.service.NotificationPublicService;
import com.bubli.personal.timer.service.TimeLogPublicService;
import com.bubli.work.schedule.service.SchedulePublicService;
import com.bubli.work.task.service.TaskPublicService;
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

	@InjectMocks
	DashboardService dashboardService;

	@Test
	void getWorkDashboardIncludesMemoSummary() {
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

		var response = dashboardService.getWorkDashboard(userId);

		assertThat(response.memoSummary()).containsExactly(
				"개인: 개인 메모",
				"프로젝트룸: 프로젝트룸 메모"
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
}
