package com.bubli.agent.dispatch;

import com.bubli.agent.entity.AgentJob;
import com.bubli.agent.repository.AgentJobRepository;
import com.bubli.agent.type.AgentJobStatus;
import com.bubli.agent.type.AgentJobType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentJobRetryDispatcherTest {

	@Test
	void retryPersistsPendingOutboxBeforePublishingAfterCommitEvent() {
		AgentJobRepository repository = mock(AgentJobRepository.class);
		AgentJobDispatchOutboxRecorder outboxRecorder = mock(AgentJobDispatchOutboxRecorder.class);
		ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
		AgentJobRetryDispatcher dispatcher = new AgentJobRetryDispatcher(
				repository, outboxRecorder, eventPublisher);
		UUID jobId = UUID.randomUUID();
		AgentJob job = failedJob(jobId);
		when(repository.findByStatusAndRetryCountLessThan(
				eq(AgentJobStatus.FAILED), eq(3), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(job)));

		assertThat(dispatcher.dispatchRetryableFailedJobs(3, 20)).isEqualTo(1);

		assertThat(job.getStatus()).isEqualTo(AgentJobStatus.PENDING);
		assertThat(job.getRetryCount()).isEqualTo(1);
		ArgumentCaptor<AgentJobDispatchCommand> commandCaptor =
				ArgumentCaptor.forClass(AgentJobDispatchCommand.class);
		verify(outboxRecorder).recordPending(commandCaptor.capture());
		assertThat(commandCaptor.getValue().jobId()).isEqualTo(jobId);
		verify(eventPublisher).publishEvent(new AgentJobDispatchEvent(commandCaptor.getValue()));
	}

	private AgentJob failedJob(UUID jobId) {
		AgentJob job = AgentJob.create(
				UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), AgentJobType.ANALYZE_RESOURCE);
		ReflectionTestUtils.setField(job, "id", jobId);
		job.markDispatchFailed("AGENT_EXECUTION_FAILED", "temporary error");
		return job;
	}
}
