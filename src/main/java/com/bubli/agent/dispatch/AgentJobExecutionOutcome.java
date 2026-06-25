package com.bubli.agent.dispatch;

import java.util.List;

public record AgentJobExecutionOutcome(
		boolean successful,
		String errorCode,
		String errorMessage,
		List<AgentJobExecutionSuggestionDraft> suggestionDrafts
) {

	public AgentJobExecutionOutcome {
		suggestionDrafts = suggestionDrafts == null ? List.of() : List.copyOf(suggestionDrafts);
	}

	public static AgentJobExecutionOutcome succeeded() {
		return new AgentJobExecutionOutcome(true, null, null, List.of());
	}

	public static AgentJobExecutionOutcome succeededWithSuggestions(
			List<AgentJobExecutionSuggestionDraft> suggestionDrafts
	) {
		return new AgentJobExecutionOutcome(true, null, null, suggestionDrafts);
	}

	public static AgentJobExecutionOutcome failed(String errorCode, String errorMessage) {
		return new AgentJobExecutionOutcome(false, errorCode, errorMessage, List.of());
	}
}
