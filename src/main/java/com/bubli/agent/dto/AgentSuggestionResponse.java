package com.bubli.agent.dto;

import com.bubli.agent.entity.AgentSuggestion;
import com.bubli.agent.type.AgentSuggestionStatus;
import com.bubli.agent.type.AgentSuggestionType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AgentSuggestionResponse(
        UUID suggestionId,
        UUID userId,
        UUID roomId,
        UUID jobId,
        UUID resourceId,
        AgentSuggestionType suggestionType,
        AgentSuggestionStatus status,
        Map<String, Object> payloadJson,
        Map<String, Object> evidenceJson,
        String downloadUrl,
        String exportUrl,
        UUID reviewedBy,
        Instant reviewedAt,
        Instant createdAt,
        Instant updatedAt
) {

    public static AgentSuggestionResponse from(AgentSuggestion suggestion) {
        return new AgentSuggestionResponse(
                suggestion.getId(),
                suggestion.getUserId(),
                suggestion.getRoomId(),
                suggestion.getJobId(),
                suggestion.getResourceId(),
                suggestion.getSuggestionType(),
                suggestion.getStatus(),
                suggestion.getPayloadJson(),
                suggestion.getEvidenceJson(),
                exportPath(suggestion),
                exportPath(suggestion),
                suggestion.getReviewedBy(),
                suggestion.getReviewedAt(),
                suggestion.getCreatedAt(),
                suggestion.getUpdatedAt()
        );
    }

    private static String exportPath(AgentSuggestion suggestion) {
        if (suggestion.getSuggestionType() != AgentSuggestionType.DOCUMENT_DRAFT) {
            return null;
        }
        return "/api/agent/suggestions/%s/export".formatted(suggestion.getId());
    }
}
