package com.bubli.agent.dispatch;

public record AgentJobExecutionOutcome(
		boolean successful,
		String errorCode,
		String errorMessage
) {

	public static AgentJobExecutionOutcome succeeded() {
		return new AgentJobExecutionOutcome(true, null, null);
	}

	public static AgentJobExecutionOutcome failed(String errorCode, String errorMessage) {
		return new AgentJobExecutionOutcome(false, errorCode, errorMessage);
	}
}
