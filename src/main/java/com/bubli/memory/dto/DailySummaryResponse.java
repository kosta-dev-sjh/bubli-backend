package com.bubli.memory.dto;

import com.bubli.memory.entity.DailySummary;
import com.bubli.memory.type.SummaryStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record DailySummaryResponse(
        UUID id,
        UUID userId,
        LocalDate summaryDate,
        String summaryJson,
        SummaryStatus status,
        Instant approvedAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static DailySummaryResponse from(DailySummary summary) {
        return new DailySummaryResponse(
                summary.getId(),
                summary.getUserId(),
                summary.getSummaryDate(),
                summary.getSummaryJson(),
                summary.getStatus(),
                summary.getApprovedAt(),
                summary.getCreatedAt(),
                summary.getUpdatedAt()
        );
    }
}
