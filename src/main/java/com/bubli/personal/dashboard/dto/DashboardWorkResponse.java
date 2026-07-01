package com.bubli.personal.dashboard.dto;

import com.bubli.personal.timer.dto.TimeLogResult;
import com.bubli.work.schedule.dto.ScheduleResult;
import com.bubli.work.task.dto.TaskResult;

import java.util.List;

public record DashboardWorkResponse(
		List<TaskResult> todayTasks,
		List<TaskResult> upcomingDeadlines,
		List<ScheduleResult> todaySchedules,
		long unreadNotificationCount,
		TimeLogResult runningTimer,
		List<String> agentSuggestionSummary,
		List<String> memoSummary
) {
}
