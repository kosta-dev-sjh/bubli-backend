package com.bubli.agent.dispatch;

import java.util.List;

public record AgentJobExecutionOutcome(
		boolean successful,
		String errorCode,
		String errorMessage,
		List<AgentJobExecutionSuggestionDraft> suggestionDrafts,
		List<AgentJobExecutionModelCallLog> modelCallLogs
) {

	public AgentJobExecutionOutcome {
		suggestionDrafts = suggestionDrafts == null ? List.of() : List.copyOf(suggestionDrafts);
		modelCallLogs = modelCallLogs == null ? List.of() : List.copyOf(modelCallLogs);
	}

	public static AgentJobExecutionOutcome succeeded() {
		return new AgentJobExecutionOutcome(true, null, null, List.of(), List.of());
	}

	public static AgentJobExecutionOutcome succeededWithSuggestions(
			List<AgentJobExecutionSuggestionDraft> suggestionDrafts
	) {
		return new AgentJobExecutionOutcome(true, null, null, suggestionDrafts, List.of());
	}

	public static AgentJobExecutionOutcome succeededWithModelCallLogs(
			List<AgentJobExecutionModelCallLog> modelCallLogs
	) {
		return new AgentJobExecutionOutcome(true, null, null, List.of(), modelCallLogs);
	}

	public static AgentJobExecutionOutcome succeededWithResults(
			List<AgentJobExecutionSuggestionDraft> suggestionDrafts,
			List<AgentJobExecutionModelCallLog> modelCallLogs
	) {
		return new AgentJobExecutionOutcome(true, null, null, suggestionDrafts, modelCallLogs);
	}

	public static AgentJobExecutionOutcome failed(String errorCode, String errorMessage) {
		return new AgentJobExecutionOutcome(false, errorCode, errorMessage, List.of(), List.of());
	}

	public static AgentJobExecutionOutcome failedWithModelCallLogs(
			String errorCode,
			String errorMessage,
			List<AgentJobExecutionModelCallLog> modelCallLogs
	) {
		return new AgentJobExecutionOutcome(false, errorCode, errorMessage, List.of(), modelCallLogs);
	}
}
