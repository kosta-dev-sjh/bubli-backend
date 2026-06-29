package com.bubli.agent.dispatch;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
@ConditionalOnProperty(name = "agent.dispatch.adapter", havingValue = "in-memory")
public class InMemoryAgentJobQueueAdapter implements AgentJobDispatchPort, AgentJobQueueConsumerPort {

	private final Queue<AgentJobQueueMessage> queue = new ConcurrentLinkedQueue<>();

	@Override
	public void dispatch(AgentJobDispatchCommand command) {
		queue.add(AgentJobQueueMessage.from(command, Instant.now()));
	}

	public Optional<AgentJobQueueMessage> poll() {
		return Optional.ofNullable(queue.poll());
	}

	public int size() {
		return queue.size();
	}
}
