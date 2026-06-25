package com.bubli.agent.dispatch;

import com.bubli.agent.type.AgentSuggestionType;

public record AgentJobExecutionSuggestionDraft(
		AgentSuggestionType suggestionType,
		String payloadJson,
		String evidenceJson
) {
}
