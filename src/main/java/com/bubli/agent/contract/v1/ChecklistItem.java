package com.bubli.agent.contract.v1;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ChecklistItem(
        @NotBlank String title,
        @NotNull ChecklistSeverity severity
) {
}
