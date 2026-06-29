package com.bubli.agent.dispatch;

import com.bubli.agent.entity.AgentJob;

public record AgentJobDispatchEvent(
		AgentJobDispatchCommand command
) {
	public static AgentJobDispatchEvent from(AgentJob agentJob) {
		return new AgentJobDispatchEvent(AgentJobDispatchCommand.from(agentJob));
	}
}
