package com.bubli.personal.dashboard.service;

import com.bubli.agent.service.AgentSuggestionPublicService;
import com.bubli.personal.dashboard.dto.DashboardWorkResponse;
import com.bubli.personal.memo.dto.MemoResult;
import com.bubli.personal.memo.service.MemoPublicService;
import com.bubli.personal.notification.service.NotificationPublicService;
import com.bubli.personal.timer.dto.TimeLogResult;
import com.bubli.personal.timer.service.TimeLogPublicService;
import com.bubli.resource.dto.ResourceAnalysisSummaryResult;
import com.bubli.resource.service.ResourcePublicService;
import com.bubli.work.schedule.service.SchedulePublicService;
import com.bubli.work.task.service.TaskPublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardService {

	private static final int DASHBOARD_MEMO_LIMIT = 5;
	private static final int DASHBOARD_MEMO_LOOKBACK_DAYS = 30;
	private static final int MEMO_SUMMARY_BODY_LIMIT = 80;
	private static final int RESOURCE_ANALYSIS_SUMMARY_LIMIT = 5;
	private static final int RESOURCE_ANALYSIS_SUMMARY_LIMIT_LENGTH = 100;

	private final TaskPublicService taskPublicService;
	private final SchedulePublicService schedulePublicService;
	private final NotificationPublicService notificationPublicService;
	private final TimeLogPublicService timeLogPublicService;
	private final AgentSuggestionPublicService agentSuggestionPublicService;
	private final MemoPublicService memoPublicService;
	private final ResourcePublicService resourcePublicService;

	@Transactional(readOnly = true)
	public DashboardWorkResponse getWorkDashboard(UUID userId) {
		Instant now = Instant.now();
		Instant startOfToday = now.truncatedTo(ChronoUnit.DAYS);
		Instant startOfTomorrow = startOfToday.plus(1, ChronoUnit.DAYS);
		Instant endOfWeek = startOfToday.plus(7, ChronoUnit.DAYS);

		TimeLogResult runningTimer = timeLogPublicService.getRunningTimer(userId).orElse(null);

		return new DashboardWorkResponse(
				taskPublicService.getDueBetweenTasks(userId, startOfToday, startOfTomorrow),
				taskPublicService.getDueBetweenTasks(userId, startOfTomorrow, endOfWeek),
				schedulePublicService.getSchedulesBetween(userId, startOfToday, startOfTomorrow),
				notificationPublicService.countUnread(userId),
				runningTimer,
				agentSuggestionPublicService.getReviewRequiredSummaries(userId, 5),
				memoPublicService.getUpdatedMemosBetween(
								userId,
								now.minus(DASHBOARD_MEMO_LOOKBACK_DAYS, ChronoUnit.DAYS),
								now.plus(1, ChronoUnit.SECONDS),
								DASHBOARD_MEMO_LIMIT
						)
						.stream()
						.map(this::memoSummaryLine)
						.toList(),
				resourcePublicService.getRecentAnalysisSummaries(userId, RESOURCE_ANALYSIS_SUMMARY_LIMIT)
						.stream()
						.map(this::resourceAnalysisSummaryLine)
						.toList()
		);
	}

	private String memoSummaryLine(MemoResult memo) {
		String scope = memo.roomId() == null ? "개인" : "프로젝트룸";
		return "%s: %s".formatted(scope, truncate(memo.body(), MEMO_SUMMARY_BODY_LIMIT));
	}

	private String truncate(String text, int limit) {
		if (text == null || text.length() <= limit) {
			return text;
		}
		return text.substring(0, limit) + "...";
	}

	private String resourceAnalysisSummaryLine(ResourceAnalysisSummaryResult summary) {
		return "자료: %s - %s".formatted(
				summary.title(),
				truncate(summary.summary(), RESOURCE_ANALYSIS_SUMMARY_LIMIT_LENGTH)
		);
	}
}
