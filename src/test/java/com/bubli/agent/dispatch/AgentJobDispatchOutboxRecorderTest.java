package com.bubli.agent.dispatch;

import com.bubli.agent.entity.AgentDispatchOutbox;
import com.bubli.agent.repository.AgentDispatchOutboxRepository;
import com.bubli.agent.type.AgentDispatchOutboxStatus;
import com.bubli.agent.type.AgentJobType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentJobDispatchOutboxRecorderTest {

	@Test
	void recordPendingStoresDispatchPayload() {
		AgentDispatchOutboxRepository repository = mock(AgentDispatchOutboxRepository.class);
		AgentJobDispatchOutboxRecorder recorder = new AgentJobDispatchOutboxRecorder(
				repository,
				new ObjectMapper()
		);
		UUID jobId = UUID.randomUUID();
		AgentJobDispatchCommand command = command(jobId);

		recorder.recordPending(command);

		ArgumentCaptor<AgentDispatchOutbox> outboxCaptor = ArgumentCaptor.forClass(AgentDispatchOutbox.class);
		verify(repository).save(outboxCaptor.capture());
		AgentDispatchOutbox outbox = outboxCaptor.getValue();
		assertThat(outbox.getJobId()).isEqualTo(jobId);
		assertThat(outbox.getStatus()).isEqualTo(AgentDispatchOutboxStatus.PENDING);
		assertThat(outbox.getRetryCount()).isZero();
		assertThat(outbox.getPayloadJson()).contains(jobId.toString());
		assertThat(outbox.getPayloadJson()).contains(AgentJobType.ANALYZE_RESOURCE.name());
	}

	@Test
	void recordDispatchedMarksOutboxDispatched() {
		AgentDispatchOutboxRepository repository = mock(AgentDispatchOutboxRepository.class);
		AgentJobDispatchOutboxRecorder recorder = new AgentJobDispatchOutboxRecorder(
				repository,
				new ObjectMapper()
		);
		UUID jobId = UUID.randomUUID();
		AgentDispatchOutbox outbox = AgentDispatchOutbox.pending(jobId, "{}");
		when(repository.findByJobId(jobId)).thenReturn(Optional.of(outbox));

		recorder.recordDispatched(jobId);

		assertThat(outbox.getStatus()).isEqualTo(AgentDispatchOutboxStatus.DISPATCHED);
		assertThat(outbox.getDispatchedAt()).isNotNull();
		assertThat(outbox.getErrorCode()).isNull();
		assertThat(outbox.getErrorMessage()).isNull();
	}

	@Test
	void recordFailureMarksOutboxFailedAndIncrementsRetryCount() {
		AgentDispatchOutboxRepository repository = mock(AgentDispatchOutboxRepository.class);
		AgentJobDispatchOutboxRecorder recorder = new AgentJobDispatchOutboxRecorder(
				repository,
				new ObjectMapper()
		);
		UUID jobId = UUID.randomUUID();
		AgentDispatchOutbox outbox = AgentDispatchOutbox.pending(jobId, "{}");
		when(repository.findByJobId(jobId)).thenReturn(Optional.of(outbox));

		recorder.recordFailure(jobId, "AGENT_DISPATCH_ENQUEUE_FAILED", "queue unavailable");

		assertThat(outbox.getStatus()).isEqualTo(AgentDispatchOutboxStatus.FAILED);
		assertThat(outbox.getRetryCount()).isEqualTo(1);
		assertThat(outbox.getErrorCode()).isEqualTo("AGENT_DISPATCH_ENQUEUE_FAILED");
		assertThat(outbox.getErrorMessage()).isEqualTo("queue unavailable");
		assertThat(outbox.getDispatchedAt()).isNull();
	}

	private AgentJobDispatchCommand command(UUID jobId) {
		return new AgentJobDispatchCommand(
				jobId,
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				AgentJobType.ANALYZE_RESOURCE
		);
	}
}
