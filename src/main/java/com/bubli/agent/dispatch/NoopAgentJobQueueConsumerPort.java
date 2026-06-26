package com.bubli.agent.dispatch;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@ConditionalOnMissingBean(AgentJobQueueConsumerPort.class)
public class NoopAgentJobQueueConsumerPort implements AgentJobQueueConsumerPort {

	@Override
	public Optional<AgentJobQueueMessage> poll() {
		return Optional.empty();
	}
}
