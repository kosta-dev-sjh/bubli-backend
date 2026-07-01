package com.bubli.widget.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SaveUsageSummaryRequest(
        @NotBlank String deviceId,
        @NotBlank String rollupKey,
        @NotNull LocalDate summaryDate,
        @NotNull UUID bubbleSettingId,
        @NotNull Integer openCount,
        @NotNull Integer interactionCount,
        @NotNull Long visibleSeconds,
        @NotNull Instant syncedAt
) {}
