package com.bubli.widget.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record UpdateWidgetItemStateRequest(
        String bubbleType,
        UUID itemId,
        String itemType,
        @NotBlank String state
) {}
