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
	void onAgentJobCreatedDispatchesCommandThroughPortAndRecordsQueuedEvent() {
		AgentJobDispatchPort dispatchPort = mock(AgentJobDispatchPort.class);
		AgentJobDispatchFailureRecorder failureRecorder = mock(AgentJobDispatchFailureRecorder.class);
		AgentJobDispatchSuccessRecorder successRecorder = mock(AgentJobDispatchSuccessRecorder.class);
		AgentJobDispatchOutboxRecorder outboxRecorder = mock(AgentJobDispatchOutboxRecorder.class);
		AgentJobDispatchEventListener listener = new AgentJobDispatchEventListener(
				dispatchPort,
				failureRecorder,
				successRecorder,
				outboxRecorder
		);
		AgentJobDispatchCommand command = new AgentJobDispatchCommand(
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				AgentJobType.ANALYZE_RESOURCE
		);

		listener.onAgentJobCreated(new AgentJobDispatchEvent(command));

		verify(dispatchPort).dispatch(command);
		verify(outboxRecorder).recordDispatched(command.jobId());
		verify(successRecorder).recordQueued(command);
		verifyNoInteractions(failureRecorder);
	}

	@Test
	void onAgentJobCreatedRecordsFailureWhenDispatchFails() {
		AgentJobDispatchPort dispatchPort = mock(AgentJobDispatchPort.class);
		AgentJobDispatchFailureRecorder failureRecorder = mock(AgentJobDispatchFailureRecorder.class);
		AgentJobDispatchSuccessRecorder successRecorder = mock(AgentJobDispatchSuccessRecorder.class);
		AgentJobDispatchOutboxRecorder outboxRecorder = mock(AgentJobDispatchOutboxRecorder.class);
		AgentJobDispatchEventListener listener = new AgentJobDispatchEventListener(
				dispatchPort,
				failureRecorder,
				successRecorder,
				outboxRecorder
		);
		AgentJobDispatchCommand command = new AgentJobDispatchCommand(
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				AgentJobType.ANALYZE_RESOURCE
		);
		RuntimeException exception = new IllegalStateException("queue unavailable");
		doThrow(exception).when(dispatchPort).dispatch(command);

		assertThatNoException().isThrownBy(() -> listener.onAgentJobCreated(new AgentJobDispatchEvent(command)));

		verify(failureRecorder).recordEnqueueFailure(command, exception);
		verify(outboxRecorder).recordFailure(
				command.jobId(),
				AgentJobDispatchFailureRecorder.ENQUEUE_FAILURE_ERROR_CODE,
				"queue unavailable"
		);
		verifyNoInteractions(successRecorder);
	}

	@Test
	void onAgentJobCreatedDoesNotMarkFailedWhenQueuedEventRecordingFails() {
		AgentJobDispatchPort dispatchPort = mock(AgentJobDispatchPort.class);
		AgentJobDispatchFailureRecorder failureRecorder = mock(AgentJobDispatchFailureRecorder.class);
		AgentJobDispatchSuccessRecorder successRecorder = mock(AgentJobDispatchSuccessRecorder.class);
		AgentJobDispatchOutboxRecorder outboxRecorder = mock(AgentJobDispatchOutboxRecorder.class);
		AgentJobDispatchEventListener listener = new AgentJobDispatchEventListener(
				dispatchPort,
				failureRecorder,
				successRecorder,
				outboxRecorder
		);
		AgentJobDispatchCommand command = new AgentJobDispatchCommand(
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				AgentJobType.ANALYZE_RESOURCE
		);
		doThrow(new IllegalStateException("event log unavailable"))
				.when(successRecorder)
				.recordQueued(command);

		assertThatNoException().isThrownBy(() -> listener.onAgentJobCreated(new AgentJobDispatchEvent(command)));

		verify(dispatchPort).dispatch(command);
		verify(outboxRecorder).recordDispatched(command.jobId());
		verify(successRecorder).recordQueued(command);
		verifyNoInteractions(failureRecorder);
	}
}
