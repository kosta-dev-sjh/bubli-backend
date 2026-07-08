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
        String value,
        String assigneeUserId,
        String wbsItemId,
        String parentId,
        Integer orderNo,
        String status,
        String startsAt,
        String dueAt,
        String endsAt,
        Boolean allDay,
        String scheduleTitle,
        String documentType,
        String contentMarkdown
) {
    public Suggestion(
            SuggestionType type,
            String title,
            String description,
            String sourceText,
            Double confidence,
            String fieldKey,
            String value,
            String assigneeUserId,
            String wbsItemId,
            String parentId,
            Integer orderNo,
            String status,
            String startsAt,
            String dueAt,
            String endsAt,
            Boolean allDay,
            String scheduleTitle
    ) {
        this(
                type,
                title,
                description,
                sourceText,
                confidence,
                fieldKey,
                value,
                assigneeUserId,
                wbsItemId,
                parentId,
                orderNo,
                status,
                startsAt,
                dueAt,
                endsAt,
                allDay,
                scheduleTitle,
                null,
                null
        );
    }

    public Suggestion(
            SuggestionType type,
            String title,
            String description,
            String sourceText,
            Double confidence,
            String fieldKey,
            String value,
            String assigneeUserId,
            String wbsItemId,
            String parentId,
            Integer orderNo,
            String status,
            String startsAt,
            String dueAt,
            String endsAt,
            Boolean allDay,
            String scheduleTitle,
            String contentMarkdown
    ) {
        this(
                type,
                title,
                description,
                sourceText,
                confidence,
                fieldKey,
                value,
                assigneeUserId,
                wbsItemId,
                parentId,
                orderNo,
                status,
                startsAt,
                dueAt,
                endsAt,
                allDay,
                scheduleTitle,
                null,
                contentMarkdown
        );
    }

    public Suggestion(
            SuggestionType type,
            String title,
            String description,
            String sourceText,
            Double confidence,
            String fieldKey,
            String value
    ) {
        this(
                type,
                title,
                description,
                sourceText,
                confidence,
                fieldKey,
                value,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}
