package com.bubli.agent.dto;

import com.bubli.agent.type.AgentSuggestionStatus;
import com.bubli.agent.type.AgentSuggestionType;

import java.time.Instant;
import java.util.UUID;

public record AgentSuggestionResponse(
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
	public static AgentSuggestionResponse from(AgentSuggestionResult result) {
		return new AgentSuggestionResponse(
				result.id(),
				result.userId(),
				result.roomId(),
				result.jobId(),
				result.resourceId(),
				result.suggestionType(),
				result.payloadJson(),
				result.evidenceJson(),
				result.status(),
				result.createdAt(),
				result.updatedAt()
		);
	}
}
