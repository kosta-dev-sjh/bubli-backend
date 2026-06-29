package com.bubli.memory.dto;

public record DailySummaryPatchCommand(
        DailySummaryAction action,
        String summaryJson
) {
}
