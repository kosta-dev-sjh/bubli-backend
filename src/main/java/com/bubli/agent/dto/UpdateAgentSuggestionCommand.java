package com.bubli.agent.dto;

import com.bubli.agent.type.AgentSuggestionStatus;

public record UpdateAgentSuggestionCommand(
		AgentSuggestionStatus status,
		String payloadJson,
		String evidenceJson
) {
}
