package com.bubli.widget.service;

import com.bubli.widget.dto.WidgetTodaySummaryResponse;

import java.time.LocalDate;
import java.util.UUID;

public interface WidgetPublicService {

    WidgetTodaySummaryResponse getUsageSummary(UUID userId, LocalDate summaryDate);
}
