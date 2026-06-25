package com.bubli.agent.contract.v1;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record AgentAnalysisResult(
        @NotBlank String schemaVersion,
        @NotNull UUID resourceId,
        @NotNull @Valid ModelInfo model,
        @NotNull @Valid Analysis analysis,
        @NotNull List<@Valid @NotNull Suggestion> suggestions
) {

    public static final String SCHEMA_VERSION = "analysis.v1";

    public AgentAnalysisResult {
        suggestions = suggestions == null ? null : List.copyOf(suggestions);
    }
}
