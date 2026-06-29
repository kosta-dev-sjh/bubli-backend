package com.bubli.agent.dispatch;

import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class NoopAgentJobExecutionPort implements AgentJobExecutionPort {

	@Override
	public Optional<AgentJobExecutionOutcome> execute(AgentJobQueueMessage message) {
		return Optional.empty();
	}
}
