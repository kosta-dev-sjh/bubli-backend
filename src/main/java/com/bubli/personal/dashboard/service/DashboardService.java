package com.bubli.personal.dashboard.service;

import com.bubli.agent.service.AgentSuggestionPublicService;
import com.bubli.personal.dashboard.dto.DashboardActivityHeatmapResponse;
import com.bubli.personal.dashboard.dto.DashboardProjectProgressSummary;
import com.bubli.personal.dashboard.dto.DashboardWorkResponse;
import com.bubli.personal.memo.dto.MemoResult;
import com.bubli.personal.memo.service.MemoPublicService;
import com.bubli.personal.notification.service.NotificationPublicService;
import com.bubli.personal.timer.dto.TimeLogActivityRow;
import com.bubli.personal.timer.dto.TimeLogResult;
import com.bubli.personal.timer.service.TimeLogPublicService;
import com.bubli.project.dto.ProjectRoomResult;
import com.bubli.project.service.ProjectRoomPublicService;
import com.bubli.resource.dto.ResourceAnalysisSummaryResult;
import com.bubli.resource.service.ResourcePublicService;
import com.bubli.work.schedule.service.SchedulePublicService;
import com.bubli.work.task.dto.TaskResult;
import com.bubli.work.task.service.TaskPublicService;
import com.bubli.work.task.type.TaskStatus;
import com.bubli.work.wbs.dto.WbsItemResult;
import com.bubli.work.wbs.service.WbsItemPublicService;
import com.bubli.work.wbs.type.WbsStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardService {

	private static final int DASHBOARD_MEMO_LIMIT = 5;
	private static final int DASHBOARD_MEMO_LOOKBACK_DAYS = 30;
	private static final int MEMO_SUMMARY_BODY_LIMIT = 80;
	private static final int RESOURCE_ANALYSIS_SUMMARY_LIMIT = 5;
	private static final int RESOURCE_ANALYSIS_SUMMARY_LIMIT_LENGTH = 100;
	private static final int PROJECT_PROGRESS_SUMMARY_LIMIT = 5;
	private static final ZoneId ACTIVITY_HEATMAP_ZONE = ZoneId.of("Asia/Seoul");
	private static final int ACTIVITY_HEATMAP_MAX_DAYS = 365;

	private final TaskPublicService taskPublicService;
	private final SchedulePublicService schedulePublicService;
	private final NotificationPublicService notificationPublicService;
	private final TimeLogPublicService timeLogPublicService;
	private final AgentSuggestionPublicService agentSuggestionPublicService;
	private final MemoPublicService memoPublicService;
	private final ResourcePublicService resourcePublicService;
	private final ProjectRoomPublicService projectRoomPublicService;
	private final WbsItemPublicService wbsItemPublicService;

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
						.toList(),
				projectRoomPublicService.getAccessibleRooms(userId, PROJECT_PROGRESS_SUMMARY_LIMIT)
						.stream()
						.map(this::projectProgressSummary)
						.toList()
		);
	}

	@Transactional(readOnly = true)
	public List<DashboardActivityHeatmapResponse> getActivityHeatmap(UUID userId, int days) {
		int boundedDays = Math.max(1, Math.min(days, ACTIVITY_HEATMAP_MAX_DAYS));
		LocalDate today = LocalDate.now(ACTIVITY_HEATMAP_ZONE);
		LocalDate startDate = today.minusDays(boundedDays - 1L);
		Instant from = startDate.atStartOfDay(ACTIVITY_HEATMAP_ZONE).toInstant();
		Instant to = today.plusDays(1).atStartOfDay(ACTIVITY_HEATMAP_ZONE).toInstant();

		Map<LocalDate, Long> activityCounts = new HashMap<>();
		Map<LocalDate, Long> focusSeconds = new HashMap<>();

		for (TimeLogActivityRow row : timeLogPublicService.getActivityBetween(userId, from, to)) {
			LocalDate date = toKstDate(row.startedAt());
			activityCounts.merge(date, 1L, Long::sum);
			focusSeconds.merge(date, row.durationSeconds() == null ? 0L : row.durationSeconds(), Long::sum);
		}
		mergeActivityDates(taskPublicService.getCompletedAtBetween(userId, from, to), activityCounts);
		mergeActivityDates(resourcePublicService.getUploadedAtBetween(userId, from, to), activityCounts);

		List<DashboardActivityHeatmapResponse> heatmap = new ArrayList<>();
		for (LocalDate date = startDate; !date.isAfter(today); date = date.plusDays(1)) {
			long count = activityCounts.getOrDefault(date, 0L);
			long focusMinutes = focusSeconds.getOrDefault(date, 0L) / 60;
			heatmap.add(new DashboardActivityHeatmapResponse(date, count, focusMinutes));
		}
		return heatmap;
	}

	private void mergeActivityDates(List<Instant> timestamps, Map<LocalDate, Long> counts) {
		for (Instant timestamp : timestamps) {
			counts.merge(toKstDate(timestamp), 1L, Long::sum);
		}
	}

	private LocalDate toKstDate(Instant instant) {
		return instant.atZone(ACTIVITY_HEATMAP_ZONE).toLocalDate();
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

	private DashboardProjectProgressSummary projectProgressSummary(ProjectRoomResult room) {
		var tasks = taskPublicService.getRoomTasksForBoard(room.id());
		var wbsItems = wbsItemPublicService.getRoomItemsForBoard(room.id());
		long total = tasks.size();
		long todo = countStatus(tasks, TaskStatus.TODO);
		long inProgress = countStatus(tasks, TaskStatus.IN_PROGRESS);
		long review = countStatus(tasks, TaskStatus.REVIEW);
		long blocked = countStatus(tasks, TaskStatus.BLOCKED);
		long done = countStatus(tasks, TaskStatus.DONE);
		long totalWbs = wbsItems.size();
		long todoWbs = countStatus(wbsItems, WbsStatus.TODO);
		long inProgressWbs = countStatus(wbsItems, WbsStatus.IN_PROGRESS);
		long doneWbs = countStatus(wbsItems, WbsStatus.DONE);
		return new DashboardProjectProgressSummary(
				room.id(),
				room.name(),
				total,
				todo,
				inProgress,
				review,
				blocked,
				done,
				progressPercent(done, total),
				totalWbs,
				todoWbs,
				inProgressWbs,
				doneWbs,
				progressPercent(doneWbs, totalWbs)
		);
	}

	private long countStatus(List<TaskResult> tasks, TaskStatus status) {
		return tasks.stream()
				.filter(task -> task.status() == status)
				.count();
	}

	private long countStatus(List<WbsItemResult> items, WbsStatus status) {
		return items.stream()
				.filter(item -> item.status() == status)
				.count();
	}

	private int progressPercent(long done, long total) {
		if (total == 0) {
			return 0;
		}
		return (int) Math.round(done * 100.0 / total);
	}
}
