package com.bubli.agent.dto;

import com.bubli.agent.type.AgentSuggestionReviewAction;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record AgentSuggestionUpdateRequest(
        @NotNull AgentSuggestionReviewAction action,
        Map<String, Object> editedContent,
        Map<String, Object> payloadJson
) {
    public Map<String, Object> effectiveEditedContent() {
        return editedContent != null ? editedContent : payloadJson;
    }
}
