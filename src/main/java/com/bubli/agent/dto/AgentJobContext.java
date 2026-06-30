package com.bubli.agent.dto;

public record AgentJobContext(
		String promptBlock,
		int characterCount
) {

	public static AgentJobContext empty() {
		return new AgentJobContext("No additional context was collected.", 0);
	}
}
