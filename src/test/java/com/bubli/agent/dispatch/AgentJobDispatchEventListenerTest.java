package com.bubli.agent.dispatch;

import com.bubli.agent.type.AgentJobType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AgentJobDispatchEventListenerTest {

	@Test
	void onAgentJobCreatedDispatchesCommandThroughPort() {
		AgentJobDispatchPort dispatchPort = mock(AgentJobDispatchPort.class);
		AgentJobDispatchEventListener listener = new AgentJobDispatchEventListener(dispatchPort);
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
}
