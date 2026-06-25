package com.bubli.agent.dto;

import com.bubli.agent.entity.AgentSuggestion;
import com.bubli.agent.type.AgentSuggestionStatus;
import com.bubli.agent.type.AgentSuggestionType;

import java.time.Instant;
import java.util.UUID;

public record AgentSuggestionResult(
		UUID id,
		UUID userId,
		UUID roomId,
		UUID jobId,
		UUID resourceId,
		AgentSuggestionType suggestionType,
		String payloadJson,
		String evidenceJson,
		AgentSuggestionStatus status,
		Instant createdAt,
		Instant updatedAt
) {
	public static AgentSuggestionResult from(AgentSuggestion suggestion) {
		return new AgentSuggestionResult(
				suggestion.getId(),
				suggestion.getUserId(),
				suggestion.getRoomId(),
				suggestion.getJobId(),
				suggestion.getResourceId(),
				suggestion.getSuggestionType(),
				suggestion.getPayloadJson(),
				suggestion.getEvidenceJson(),
				suggestion.getStatus(),
				suggestion.getCreatedAt(),
				suggestion.getUpdatedAt()
		);
	}
}
