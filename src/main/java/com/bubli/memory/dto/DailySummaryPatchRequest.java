package com.bubli.memory.dto;

import jakarta.validation.constraints.NotNull;

public record DailySummaryPatchRequest(
        @NotNull DailySummaryAction action,
        String summaryJson
) {
    public DailySummaryPatchCommand toCommand() {
        return new DailySummaryPatchCommand(action, summaryJson);
    }
}
