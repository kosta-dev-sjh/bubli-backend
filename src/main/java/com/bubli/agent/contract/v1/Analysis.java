package com.bubli.agent.contract.v1;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record Analysis(
        @NotBlank String summary,
        @NotNull List<@NotBlank String> keywords,
        @NotNull List<@NotBlank String> risks,
        @NotNull List<@Valid @NotNull ChecklistItem> checklist
) {

    public Analysis {
        keywords = keywords == null ? null : List.copyOf(keywords);
        risks = risks == null ? null : List.copyOf(risks);
        checklist = checklist == null ? null : List.copyOf(checklist);
    }
}
