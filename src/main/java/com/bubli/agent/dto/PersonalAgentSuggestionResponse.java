package com.bubli.agent.dto;

import com.bubli.agent.type.AgentSuggestionType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record PersonalAgentSuggestionResponse(
		UUID localSuggestionId,
		AgentSuggestionType suggestionType,
		Map<String, Object> payload,
		Map<String, Object> evidence
) {
	public PersonalAgentSuggestionResponse {
		payload = payload == null ? Map.of() : new LinkedHashMap<>(payload);
		evidence = evidence == null ? Map.of() : new LinkedHashMap<>(evidence);
	}
}
