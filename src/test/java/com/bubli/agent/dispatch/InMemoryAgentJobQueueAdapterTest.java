package com.bubli.agent.dispatch;

import com.bubli.agent.type.AgentJobType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryAgentJobQueueAdapterTest {

	@Test
	void queueMessageCopiesDispatchCommandFields() {
		Instant enqueuedAt = Instant.now();
		AgentJobDispatchCommand command = new AgentJobDispatchCommand(
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				AgentJobType.ANALYZE_RESOURCE,
				Map.of("summaryDate", "2026-07-01")
		);

		AgentJobQueueMessage message = AgentJobQueueMessage.from(command, enqueuedAt);

		assertThat(message.jobId()).isEqualTo(command.jobId());
		assertThat(message.requestedByUserId()).isEqualTo(command.requestedByUserId());
		assertThat(message.roomId()).isEqualTo(command.roomId());
		assertThat(message.resourceId()).isEqualTo(command.resourceId());
		assertThat(message.jobType()).isEqualTo(command.jobType());
		assertThat(message.requestPayload()).containsEntry("summaryDate", "2026-07-01");
		assertThat(message.enqueuedAt()).isEqualTo(enqueuedAt);
	}

	@Test
	void dispatchEnqueuesMessageWithoutExecutingJob() {
		InMemoryAgentJobQueueAdapter adapter = new InMemoryAgentJobQueueAdapter();
		AgentJobDispatchCommand command = new AgentJobDispatchCommand(
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				AgentJobType.GENERATE_TASKS
		);

		adapter.dispatch(command);

		assertThat(adapter.size()).isEqualTo(1);
		assertThat(adapter.poll())
				.hasValueSatisfying(message -> {
					assertThat(message.jobId()).isEqualTo(command.jobId());
					assertThat(message.jobType()).isEqualTo(AgentJobType.GENERATE_TASKS);
					assertThat(message.enqueuedAt()).isNotNull();
				});
		assertThat(adapter.size()).isZero();
	}
}
