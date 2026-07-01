package com.bubli.widget.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record WidgetDailySummaryResponse(
        UUID id,
        String deviceId,
        LocalDate summaryDate,
        UUID bubbleSettingId,
        int openCount,
        int interactionCount,
        long visibleSeconds,
        Instant syncedAt
) {}
