package com.bubli.agent.dto;

import com.bubli.agent.type.AgentSuggestionType;

import java.util.UUID;

public record CreateAgentSuggestionCommand(
		UUID userId,
		UUID roomId,
		UUID jobId,
		UUID resourceId,
		AgentSuggestionType suggestionType,
		String payloadJson,
		String evidenceJson
) {
}
