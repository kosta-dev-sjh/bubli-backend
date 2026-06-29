package com.bubli.memory.dto;

import java.time.LocalDate;

public record CreateDailySummaryDraftCommand(
		LocalDate summaryDate,
		String summaryJson
) {
}
