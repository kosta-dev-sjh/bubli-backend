package com.bubli.agent.dispatch;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@ConditionalOnProperty(name = "agent.dispatch.adapter", havingValue = "noop", matchIfMissing = true)
public class NoopAgentJobQueueConsumerPort implements AgentJobQueueConsumerPort {

	@Override
	public Optional<AgentJobQueueMessage> poll() {
		return Optional.empty();
	}
}
