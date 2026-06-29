package com.bubli.agent.contract.v1;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record Suggestion(
        @NotNull SuggestionType type,
        String title,
        String description,
        String sourceText,
        @DecimalMin("0.0") @DecimalMax("1.0") Double confidence,
        String fieldKey,
        String value
) {
}
