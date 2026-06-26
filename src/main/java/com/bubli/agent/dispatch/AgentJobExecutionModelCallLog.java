package com.bubli.agent.dispatch;

public record AgentJobExecutionModelCallLog(
		String promptVersion,
		String schemaVersion,
		String modelName,
		Long latencyMs,
		Integer inputTokens,
		Integer outputTokens,
		String errorCode
) {
}
