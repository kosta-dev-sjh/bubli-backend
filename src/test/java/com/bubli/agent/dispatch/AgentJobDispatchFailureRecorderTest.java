package com.bubli.agent.dispatch;

import com.bubli.agent.entity.AgentJob;
import com.bubli.agent.repository.AgentJobRepository;
import com.bubli.agent.type.AgentJobStatus;
import com.bubli.agent.type.AgentJobType;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentJobDispatchFailureRecorderTest {

	@Test
	void recordEnqueueFailureMarksJobFailed() {
		AgentJobRepository agentJobRepository = mock(AgentJobRepository.class);
		AgentJobDispatchFailureRecorder recorder = new AgentJobDispatchFailureRecorder(agentJobRepository);
		UUID jobId = UUID.randomUUID();
		AgentJob agentJob = AgentJob.create(
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				AgentJobType.ANALYZE_RESOURCE
		);
		ReflectionTestUtils.setField(agentJob, "id", jobId);
		when(agentJobRepository.findById(jobId)).thenReturn(Optional.of(agentJob));
		AgentJobDispatchCommand command = new AgentJobDispatchCommand(
				jobId,
				agentJob.getRequestedByUserId(),
				agentJob.getRoomId(),
				agentJob.getResourceId(),
				agentJob.getJobType()
		);

		recorder.recordEnqueueFailure(command, new IllegalStateException("queue unavailable"));

		assertThat(agentJob.getStatus()).isEqualTo(AgentJobStatus.FAILED);
		assertThat(agentJob.getErrorCode())
				.isEqualTo(AgentJobDispatchFailureRecorder.ENQUEUE_FAILURE_ERROR_CODE);
		assertThat(agentJob.getErrorMessage()).isEqualTo("queue unavailable");
		assertThat(agentJob.getFinishedAt()).isNotNull();
	}

	@Test
	void recordEnqueueFailureIgnoresMissingJob() {
		AgentJobRepository agentJobRepository = mock(AgentJobRepository.class);
		AgentJobDispatchFailureRecorder recorder = new AgentJobDispatchFailureRecorder(agentJobRepository);
		UUID jobId = UUID.randomUUID();
		when(agentJobRepository.findById(jobId)).thenReturn(Optional.empty());
		AgentJobDispatchCommand command = new AgentJobDispatchCommand(
				jobId,
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				AgentJobType.GENERATE_TASKS
		);

		recorder.recordEnqueueFailure(command, new IllegalStateException());

		verify(agentJobRepository).findById(jobId);
	}
}
