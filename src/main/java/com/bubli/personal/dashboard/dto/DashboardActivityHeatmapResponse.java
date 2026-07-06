package com.bubli.personal.dashboard.dto;

import java.time.LocalDate;

public record DashboardActivityHeatmapResponse(
		LocalDate date,
		long count,
		long focusMinutes
) {
}
