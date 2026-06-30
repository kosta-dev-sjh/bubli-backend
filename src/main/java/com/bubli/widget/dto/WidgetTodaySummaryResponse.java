package com.bubli.widget.dto;

import java.time.LocalDate;
import java.util.List;

public record WidgetTodaySummaryResponse(
        LocalDate date,
        int totalOpenCount,
        int totalInteractionCount,
        long totalVisibleSeconds,
        List<WidgetDailySummaryResponse> byDevice
) {}
