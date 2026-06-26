package com.bubli.agent.dispatch;

import com.bubli.agent.entity.AgentJob;
import com.bubli.agent.repository.AgentJobRepository;
import com.bubli.agent.type.AgentJobStatus;
import com.bubli.agent.type.AgentJobType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AgentJobRetryDispatcherTest {

	@Test
	void dispatchRetryableFailedJobsRequeuesFailedJobBelowRetryLimit() {
		AgentJobRepository agentJobRepository = mock(AgentJobRepository.class);
		AgentJobDispatchPort dispatchPort = mock(AgentJobDispatchPort.class);
		AgentJobDispatchFailureRecorder failureRecorder = mock(AgentJobDispatchFailureRecorder.class);
		AgentJobDispatchSuccessRecorder successRecorder = mock(AgentJobDispatchSuccessRecorder.class);
		AgentJobRetryDispatcher retryDispatcher = new AgentJobRetryDispatcher(
				agentJobRepository,
				dispatchPort,
				failureRecorder,
				successRecorder
		);
		UUID jobId = UUID.randomUUID();
		AgentJob agentJob = failedJob(jobId);
		when(agentJobRepository.findByStatusAndRetryCountLessThan(
				eq(AgentJobStatus.FAILED),
				eq(3),
				any(Pageable.class)
		)).thenReturn(new PageImpl<>(List.of(agentJob)));

		int dispatchedCount = retryDispatcher.dispatchRetryableFailedJobs(3, 20);

		assertThat(dispatchedCount).isEqualTo(1);
		assertThat(agentJob.getStatus()).isEqualTo(AgentJobStatus.PENDING);
		assertThat(agentJob.getErrorCode()).isNull();
		assertThat(agentJob.getErrorMessage()).isNull();
		assertThat(agentJob.getFinishedAt()).isNull();
		assertThat(agentJob.getRetryCount()).isEqualTo(1);

		ArgumentCaptor<AgentJobDispatchCommand> commandCaptor =
				ArgumentCaptor.forClass(AgentJobDispatchCommand.class);
		verify(dispatchPort).dispatch(commandCaptor.capture());
		assertThat(commandCaptor.getValue().jobId()).isEqualTo(jobId);
		verify(successRecorder).recordQueued(commandCaptor.getValue());
		verifyNoInteractions(failureRecorder);
	}

	@Test
	void dispatchRetryableFailedJobsRecordsFailureWhenRetryDispatchFails() {
		AgentJobRepository agentJobRepository = mock(AgentJobRepository.class);
		AgentJobDispatchPort dispatchPort = mock(AgentJobDispatchPort.class);
		AgentJobDispatchFailureRecorder failureRecorder = mock(AgentJobDispatchFailureRecorder.class);
		AgentJobDispatchSuccessRecorder successRecorder = mock(AgentJobDispatchSuccessRecorder.class);
		AgentJobRetryDispatcher retryDispatcher = new AgentJobRetryDispatcher(
				agentJobRepository,
				dispatchPort,
				failureRecorder,
				successRecorder
		);
		UUID jobId = UUID.randomUUID();
		AgentJob agentJob = failedJob(jobId);
		RuntimeException exception = new IllegalStateException("queue unavailable");
		when(agentJobRepository.findByStatusAndRetryCountLessThan(
				eq(AgentJobStatus.FAILED),
				eq(3),
				any(Pageable.class)
		)).thenReturn(new PageImpl<>(List.of(agentJob)));
		doThrow(exception).when(dispatchPort).dispatch(any(AgentJobDispatchCommand.class));

		int dispatchedCount = retryDispatcher.dispatchRetryableFailedJobs(3, 20);

		assertThat(dispatchedCount).isZero();
		assertThat(agentJob.getStatus()).isEqualTo(AgentJobStatus.FAILED);

		ArgumentCaptor<AgentJobDispatchCommand> commandCaptor =
				ArgumentCaptor.forClass(AgentJobDispatchCommand.class);
		verify(dispatchPort).dispatch(commandCaptor.capture());
		verify(failureRecorder).recordEnqueueFailure(commandCaptor.getValue(), exception);
		verifyNoInteractions(successRecorder);
	}

	private AgentJob failedJob(UUID jobId) {
		AgentJob agentJob = AgentJob.create(
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				AgentJobType.ANALYZE_RESOURCE
		);
		ReflectionTestUtils.setField(agentJob, "id", jobId);
		agentJob.markDispatchFailed("AGENT_DISPATCH_ENQUEUE_FAILED", "queue unavailable");
		return agentJob;
	}
}
