package com.bubli.agent.dispatch;

import com.bubli.agent.type.AgentJobType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NoopAgentJobExecutionPortTest {

	@Test
	void executeReturnsEmptyOutcome() {
		NoopAgentJobExecutionPort executionPort = new NoopAgentJobExecutionPort();
		AgentJobQueueMessage message = new AgentJobQueueMessage(
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				AgentJobType.ANALYZE_RESOURCE,
				Instant.now()
		);

		Optional<AgentJobExecutionOutcome> outcome = executionPort.execute(message);

		assertThat(outcome).isEmpty();
	}
}
