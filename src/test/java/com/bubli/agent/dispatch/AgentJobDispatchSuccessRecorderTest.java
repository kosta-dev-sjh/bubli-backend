package com.bubli.agent.dispatch;

import com.bubli.agent.entity.AgentJobEvent;
import com.bubli.agent.repository.AgentJobEventRepository;
import com.bubli.agent.type.AgentJobType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AgentJobDispatchSuccessRecorderTest {

	@Test
	void recordQueuedStoresQueuedEvent() {
		AgentJobEventRepository agentJobEventRepository = mock(AgentJobEventRepository.class);
		AgentJobDispatchSuccessRecorder recorder = new AgentJobDispatchSuccessRecorder(agentJobEventRepository);
		UUID jobId = UUID.randomUUID();
		AgentJobDispatchCommand command = new AgentJobDispatchCommand(
				jobId,
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				AgentJobType.ANALYZE_RESOURCE
		);

		recorder.recordQueued(command);

		ArgumentCaptor<AgentJobEvent> eventCaptor = ArgumentCaptor.forClass(AgentJobEvent.class);
		verify(agentJobEventRepository).save(eventCaptor.capture());
		assertThat(eventCaptor.getValue().getJobId()).isEqualTo(jobId);
		assertThat(eventCaptor.getValue().getEventType())
				.isEqualTo(AgentJobDispatchSuccessRecorder.QUEUED_EVENT_TYPE);
		assertThat(eventCaptor.getValue().getMessage())
				.isEqualTo(AgentJobDispatchSuccessRecorder.QUEUED_EVENT_MESSAGE);
	}
}
