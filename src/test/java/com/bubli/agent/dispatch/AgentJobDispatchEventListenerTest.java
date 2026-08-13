package com.bubli.agent.dispatch;

import com.bubli.agent.type.AgentJobType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class AgentJobDispatchEventListenerTest {

	@Test
	void afterCommitFastPathDispatchesAndCompletesOutbox() {
		AgentJobDispatchPort dispatchPort = mock(AgentJobDispatchPort.class);
		AgentJobDispatchSuccessRecorder successRecorder = mock(AgentJobDispatchSuccessRecorder.class);
		AgentJobDispatchOutboxRecorder outboxRecorder = mock(AgentJobDispatchOutboxRecorder.class);
		AgentJobDispatchEventListener listener = new AgentJobDispatchEventListener(
				dispatchPort, successRecorder, outboxRecorder);
		AgentJobDispatchCommand command = command();

		listener.onAgentJobCreated(new AgentJobDispatchEvent(command));

		verify(dispatchPort).dispatch(command);
		verify(outboxRecorder).recordDispatched(command.jobId());
		verify(successRecorder).recordQueued(command);
	}

	@Test
	void dispatchFailureLeavesJobPendingAndDelegatesRetryToOutbox() {
		AgentJobDispatchPort dispatchPort = mock(AgentJobDispatchPort.class);
		AgentJobDispatchSuccessRecorder successRecorder = mock(AgentJobDispatchSuccessRecorder.class);
		AgentJobDispatchOutboxRecorder outboxRecorder = mock(AgentJobDispatchOutboxRecorder.class);
		AgentJobDispatchEventListener listener = new AgentJobDispatchEventListener(
				dispatchPort, successRecorder, outboxRecorder);
		AgentJobDispatchCommand command = command();
		doThrow(new IllegalStateException("queue unavailable")).when(dispatchPort).dispatch(command);

		assertThatNoException().isThrownBy(() -> listener.onAgentJobCreated(new AgentJobDispatchEvent(command)));

		verify(outboxRecorder).recordFailure(
				command.jobId(), AgentJobDispatchFailureRecorder.ENQUEUE_FAILURE_ERROR_CODE, "queue unavailable");
		verifyNoInteractions(successRecorder);
	}

	private AgentJobDispatchCommand command() {
		return new AgentJobDispatchCommand(
				UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
				AgentJobType.ANALYZE_RESOURCE);
	}
}
