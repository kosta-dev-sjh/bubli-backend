package com.bubli.agent.dispatch;

import com.bubli.agent.type.AgentJobType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AgentJobDispatchEventListenerTest {

	@Test
	void onAgentJobCreatedDispatchesCommandThroughPort() {
		AgentJobDispatchPort dispatchPort = mock(AgentJobDispatchPort.class);
		AgentJobDispatchFailureRecorder failureRecorder = mock(AgentJobDispatchFailureRecorder.class);
		AgentJobDispatchEventListener listener = new AgentJobDispatchEventListener(dispatchPort, failureRecorder);
		AgentJobDispatchCommand command = new AgentJobDispatchCommand(
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				AgentJobType.ANALYZE_RESOURCE
		);

		listener.onAgentJobCreated(new AgentJobDispatchEvent(command));

		verify(dispatchPort).dispatch(command);
	}

	@Test
	void onAgentJobCreatedRecordsFailureWhenDispatchFails() {
		AgentJobDispatchPort dispatchPort = mock(AgentJobDispatchPort.class);
		AgentJobDispatchFailureRecorder failureRecorder = mock(AgentJobDispatchFailureRecorder.class);
		AgentJobDispatchEventListener listener = new AgentJobDispatchEventListener(dispatchPort, failureRecorder);
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
	}
}
