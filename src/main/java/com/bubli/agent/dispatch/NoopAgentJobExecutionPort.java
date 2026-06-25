package com.bubli.agent.dispatch;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@ConditionalOnMissingBean(AgentJobExecutionPort.class)
public class NoopAgentJobExecutionPort implements AgentJobExecutionPort {

	@Override
	public Optional<AgentJobExecutionOutcome> execute(AgentJobQueueMessage message) {
		return Optional.empty();
	}
}
