package com.bubli.personal.dashboard.dto;

import java.util.UUID;

public record DashboardProjectProgressSummary(
		UUID roomId,
		String roomName,
		long totalTasks,
		long todoTasks,
		long inProgressTasks,
		long reviewTasks,
		long blockedTasks,
		long doneTasks,
		int progressPercent,
		long totalWbsItems,
		long todoWbsItems,
		long inProgressWbsItems,
		long doneWbsItems,
		int wbsProgressPercent
) {
}
