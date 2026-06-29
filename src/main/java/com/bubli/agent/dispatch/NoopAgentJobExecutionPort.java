package com.bubli.agent.dispatch;

import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.Optional;

@Component
@ConditionalOnProperty(name = "agent.execution.mode", havingValue = "noop")
public class NoopAgentJobExecutionPort implements AgentJobExecutionPort {

	@Override
	public Optional<AgentJobExecutionOutcome> execute(AgentJobQueueMessage message) {
		return Optional.empty();
	}
}
